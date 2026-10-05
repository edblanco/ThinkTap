package com.dosparta.trivia.sdk

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.NonCancellable
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

/**
 * One game controller per consumer. Commands are serialized and execute in the caller's
 * coroutine scope; the SDK owns no background jobs. Collect [state] and [persistenceFailure].
 */
@Suppress("TooManyFunctions")
class TriviaSdk(
    private val repository: ITriviaRepository,
    private val sessions: IGameSessionRepository,
    private val localization: IContentLocalizationRepository,
    private val clock: GameClock = GameClock.SYSTEM
) {
    private val engine = GameEngine()
    private val commands = Mutex()
    private val categoryRequests = Mutex()
    private val mutableState = MutableStateFlow<TriviaState>(TriviaState.Idle)
    val state: StateFlow<TriviaState> = mutableState.asStateFlow()
    private val mutablePersistenceFailure = MutableStateFlow<TriviaSdkException?>(null)
    val persistenceFailure: StateFlow<TriviaSdkException?> = mutablePersistenceFailure.asStateFlow()
    val translationUnavailable: StateFlow<Boolean> = localization.translationUnavailable
    private val mutableReplayQuestions = MutableStateFlow<List<TriviaQuestion>>(emptyList())
    private var replayQuestions: List<TriviaQuestion>
        get() = mutableReplayQuestions.value
        set(value) { mutableReplayQuestions.value = value }
    val canReplay: Boolean get() = replayQuestions.isNotEmpty()
    private var accumulatedMillis = 0L
    private var runningSince: Long? = null
    private var selectedAnswers: Map<Int, String> = emptyMap()

    fun setContentLanguage(language: AppLanguage): Boolean {
        if (localization.contentLanguage.value == language) return false
        localization.setContentLanguage(language)
        return true
    }

    @Throws(TriviaSdkException::class, CancellationException::class)
    suspend fun loadCategories(): List<TriviaCategory> = categoryRequests.withLock {
        request { repository.getCategories() }
    }

    /** Returns true when an unfinished saved game was restored. Storage failures are thrown. */
    @Throws(TriviaSdkException::class, CancellationException::class)
    suspend fun restore(): Boolean = commands.withLock {
        val saved = try {
            sessions.getActiveSession()
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            throw TriviaSdkException(TriviaError.PERSISTENCE, e)
        }
        if (saved == null || saved.questions.isEmpty()) return@withLock false
        if (saved.currentIndex !in 0 until saved.questions.size ||
            saved.correctCount !in 0..saved.currentIndex
        ) {
            throw TriviaSdkException(TriviaError.PERSISTENCE, IllegalArgumentException("Invalid saved game"))
        }
        currentCoroutineContext().ensureActive()
        accumulatedMillis = normalizeSavedElapsed(saved.activeElapsedMillis)
        runningSince = clock.elapsedTimeMillis()
        selectedAnswers = saved.selectedAnswers.toMap()
        replayQuestions = saved.questions.toList()
        mutableState.value = TriviaState.Playing(
            GameSession(
                questions = replayQuestions,
                currentIndex = saved.currentIndex,
                correctCount = saved.correctCount,
                startTimeMillis = clock.wallTimeMillis() - accumulatedMillis
            )
        )
        true
    }

    suspend fun start(config: TriviaConfig = TriviaConfig()) = commands.withLock {
        val previous = mutableState.value
        mutableState.value = TriviaState.Loading
        val questions = try {
            request { repository.getQuestions(config) }.also {
                currentCoroutineContext().ensureActive()
                if (it.isEmpty()) throw TriviaSdkException(TriviaError.NO_QUESTIONS)
            }
        } catch (e: CancellationException) {
            mutableState.value = previous
            throw e
        } catch (e: TriviaSdkException) {
            mutableState.value = TriviaState.Failed(e)
            return@withLock
        }
        begin(questions)
    }

    suspend fun submit(answer: String) = commands.withLock {
        val current = (mutableState.value as? TriviaState.Playing)?.session
        if (current == null) {
            if (mutableState.value !is TriviaState.Finished) {
                mutableState.value = TriviaState.Failed(TriviaSdkException(TriviaError.NO_QUESTIONS))
            }
            return@withLock
        }
        selectedAnswers = selectedAnswers + (current.currentIndex to answer)
        val updated = engine.submitAnswer(current, answer)
        if (engine.hasMoreQuestions(updated)) {
            mutableState.value = TriviaState.Playing(updated)
            persist(updated)
        } else {
            finish(updated)
        }
    }

    suspend fun finishEarly() = commands.withLock {
        val current = (mutableState.value as? TriviaState.Playing)?.session ?: return@withLock
        finish(current)
    }

    suspend fun replay(): Boolean = commands.withLock {
        if (!canReplay) return@withLock false
        begin(replayQuestions)
        true
    }

    suspend fun reset() = commands.withLock {
        runningSince = null
        accumulatedMillis = 0L
        selectedAnswers = emptyMap()
        mutableState.value = TriviaState.Idle
        writeSession { sessions.clearActiveSession() }
    }

    suspend fun pause() = commands.withLock {
        val current = (mutableState.value as? TriviaState.Playing)?.session ?: return@withLock
        accumulatedMillis = activeElapsedMillis()
        runningSince = null
        persist(current)
    }

    suspend fun resume() = commands.withLock {
        val current = (mutableState.value as? TriviaState.Playing)?.session ?: return@withLock
        if (runningSince != null) return@withLock
        runningSince = clock.elapsedTimeMillis()
        mutableState.value = TriviaState.Playing(
            current.copy(startTimeMillis = clock.wallTimeMillis() - accumulatedMillis)
        )
    }

    private suspend fun begin(questions: List<TriviaQuestion>) {
        replayQuestions = questions.toList()
        accumulatedMillis = 0L
        runningSince = clock.elapsedTimeMillis()
        selectedAnswers = emptyMap()
        val session = GameSession(questions = replayQuestions, startTimeMillis = clock.wallTimeMillis())
        mutableState.value = TriviaState.Playing(session)
        persist(session)
    }

    private suspend fun finish(session: GameSession) {
        val elapsed = activeElapsedMillis()
        runningSince = null
        accumulatedMillis = elapsed
        replayQuestions = session.questions
        mutableState.value = TriviaState.Finished(
            GameResult(session.questions.size, session.correctCount, elapsed)
        )
        writeSession { sessions.clearActiveSession() }
    }

    private fun activeElapsedMillis(): Long =
        accumulatedMillis + (runningSince?.let { (clock.elapsedTimeMillis() - it).coerceAtLeast(0L) } ?: 0L)

    private suspend fun persist(session: GameSession) {
        val elapsed = activeElapsedMillis()
        writeSession {
            sessions.saveGameSession(
                session.questions, session.currentIndex, session.correctCount, elapsed, selectedAnswers
            )
        }
    }

    // Once a state transition has committed, finish its write before releasing the command lock.
    private suspend fun writeSession(write: suspend () -> Unit) = withContext(NonCancellable) {
        try {
            write()
            mutablePersistenceFailure.value = null
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            mutablePersistenceFailure.value = TriviaSdkException(TriviaError.PERSISTENCE, e)
        }
    }

    private suspend fun <T> request(block: suspend () -> T): T = try {
        block()
    } catch (e: CancellationException) {
        throw e
    } catch (e: TriviaSdkException) {
        throw e
    } catch (e: IllegalArgumentException) {
        throw TriviaSdkException(TriviaError.INVALID_CONFIGURATION, e)
    } catch (e: Exception) {
        throw TriviaSdkException(if (isNetworkFailure(e)) TriviaError.NETWORK else TriviaError.UNKNOWN, e)
    }

    private fun normalizeSavedElapsed(rawMillis: Long): Long =
        if (rawMillis > LEGACY_TIMESTAMP_THRESHOLD) {
            (clock.wallTimeMillis() - rawMillis).coerceAtLeast(0L)
        } else {
            rawMillis.coerceAtLeast(0L)
        }

    private companion object {
        const val LEGACY_TIMESTAMP_THRESHOLD = 30L * 24L * 60L * 60L * 1_000L
    }
}
