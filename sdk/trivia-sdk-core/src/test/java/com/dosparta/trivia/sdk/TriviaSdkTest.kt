package com.dosparta.trivia.sdk

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.IOException

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaSdkTest {
    private val question = TriviaQuestion(
        category = "Science", type = "multiple", difficulty = "easy",
        question = "What is H2O?", correctAnswer = "Water",
        options = listOf("Water", "Salt", "Sugar", "Ice")
    )
    private val clock = FakeClock()
    private val repository = MemoryTriviaRepository(listOf(question, question))
    private val sessions = MemorySessions()
    private val localization = MemoryLocalization()
    private val sdk = TriviaSdk(repository, sessions, localization, clock)

    @Test
    fun `start persists and answers advance then finish and clear`() = runTest {
        sdk.start()
        assertEquals(0, sessions.saved?.currentIndex)
        sdk.submit("  WATER  ")
        assertEquals(1, sessions.saved?.currentIndex)
        assertEquals(1, sessions.saved?.correctCount)
        assertEquals(mapOf(0 to "  WATER  "), sessions.saved?.selectedAnswers)
        clock.advance(2_000)
        sdk.submit("Salt")
        val result = (sdk.state.value as TriviaState.Finished).result
        assertEquals(2, result.totalQuestions)
        assertEquals(1, result.correctAnswers)
        assertEquals(2_000L, result.durationMillis)
        assertNull(sessions.saved)
        sdk.submit("Water")
        assertEquals(result, (sdk.state.value as TriviaState.Finished).result)
    }

    @Test
    fun `early finish counts unanswered questions and replay avoids another fetch`() = runTest {
        assertFalse(sdk.replay())
        sdk.finishEarly()
        assertEquals(TriviaState.Idle, sdk.state.value)
        sdk.start()
        sdk.submit("Water")
        sdk.finishEarly()
        assertEquals(1, (sdk.state.value as TriviaState.Finished).result.incorrectAnswers)
        assertTrue(sdk.canReplay)
        assertTrue(sdk.replay())
        assertEquals(0, (sdk.state.value as TriviaState.Playing).session.correctCount)
        assertEquals(1, repository.questionRequests)
        assertEquals(emptyMap<Int, String>(), sessions.saved?.selectedAnswers)
    }

    @Test
    fun `reset clears stored session and timing`() = runTest {
        sdk.start()
        clock.advance(400)
        sdk.reset()
        assertEquals(TriviaState.Idle, sdk.state.value)
        assertNull(sessions.saved)
        assertTrue(sdk.replay())
        sdk.finishEarly()
        assertEquals(0L, (sdk.state.value as TriviaState.Finished).result.durationMillis)
    }

    @Test
    fun `pause and repeated pause exclude background time`() = runTest {
        sdk.start()
        clock.advance(200)
        sdk.pause()
        clock.advance(10_000)
        sdk.pause()
        assertEquals(200L, sessions.saved?.activeElapsedMillis)
        sdk.resume()
        sdk.resume()
        clock.advance(300)
        sdk.finishEarly()
        assertEquals(500L, (sdk.state.value as TriviaState.Finished).result.durationMillis)
    }

    @Test
    fun `wall clock changes do not affect active duration`() = runTest {
        sdk.start()
        clock.wall -= 600_000
        clock.elapsed += 250
        sdk.finishEarly()
        assertEquals(250L, (sdk.state.value as TriviaState.Finished).result.durationMillis)
    }

    @Test
    fun `restoration preserves questions counters answers and active time`() = runTest {
        sessions.saved = GameSessionState(listOf(question, question), 1, 1, 42, mapOf(0 to "Water"))
        assertTrue(sdk.restore())
        assertEquals(1, (sdk.state.value as TriviaState.Playing).session.currentIndex)
        clock.advance(8)
        sdk.pause()
        assertEquals(50L, sessions.saved?.activeElapsedMillis)
        assertEquals(mapOf(0 to "Water"), sessions.saved?.selectedAnswers)
        sdk.submit("Water")
        assertEquals(50L, (sdk.state.value as TriviaState.Finished).result.durationMillis)
    }

    @Test
    fun `legacy saved epoch is converted to elapsed time`() = runTest {
        sessions.saved = GameSessionState(listOf(question), 0, 0, clock.wall - 123, emptyMap())
        assertTrue(sdk.restore())
        sdk.finishEarly()
        assertEquals(123L, (sdk.state.value as TriviaState.Finished).result.durationMillis)
    }

    @Test
    fun `negative and future saved time clamp to zero`() = runTest {
        for (time in listOf(-20L, clock.wall + 100)) {
            sessions.saved = GameSessionState(listOf(question), 0, 0, time, emptyMap())
            assertTrue(sdk.restore())
            sdk.finishEarly()
            assertEquals(0L, (sdk.state.value as TriviaState.Finished).result.durationMillis)
        }
    }

    @Test
    fun `no saved questions means no restored game`() = runTest {
        assertFalse(sdk.restore())
        sessions.saved = GameSessionState(emptyList(), 0, 0, 0, emptyMap())
        assertFalse(sdk.restore())
    }

    @Test
    fun `corrupt saved counters report persistence error`() = runTest {
        for ((index, score) in listOf(-1 to 0, 1 to 0, 0 to 1, 0 to -1)) {
            sessions.saved = GameSessionState(listOf(question), index, score, 0, emptyMap())
            try {
                sdk.restore()
                fail("Expected invalid saved game to fail")
            } catch (e: TriviaSdkException) {
                assertEquals(TriviaError.PERSISTENCE, e.error)
                assertTrue(e.cause is IllegalArgumentException)
            }
        }
    }

    @Test
    fun `restoration errors are not silently treated as no saved game`() = runTest {
        sessions.readFailure = IOException("storage unavailable")
        try {
            sdk.restore()
            fail("Expected storage error")
        } catch (e: TriviaSdkException) {
            assertEquals(TriviaError.PERSISTENCE, e.error)
            assertEquals(sessions.readFailure, e.cause)
        }
    }

    @Test
    fun `failed writes are observable without losing gameplay and recover on successful write`() = runTest {
        sessions.writeFailure = IOException("disk full")
        sdk.start()
        assertTrue(sdk.state.value is TriviaState.Playing)
        assertEquals(TriviaError.PERSISTENCE, sdk.persistenceFailure.value?.error)
        sessions.writeFailure = null
        sdk.pause()
        assertNull(sdk.persistenceFailure.value)
        sessions.writeFailure = IOException("cannot clear")
        sdk.finishEarly()
        assertTrue(sdk.state.value is TriviaState.Finished)
        assertEquals(TriviaError.PERSISTENCE, sdk.persistenceFailure.value?.error)
    }

    @Test
    fun `empty questions and invalid commands report explicit failures`() = runTest {
        sdk.submit("Water")
        assertEquals(TriviaError.NO_QUESTIONS, (sdk.state.value as TriviaState.Failed).failure.error)
        repository.questions = emptyList()
        sdk.start()
        assertEquals(TriviaError.NO_QUESTIONS, (sdk.state.value as TriviaState.Failed).failure.error)
        sdk.pause()
        sdk.resume()
    }

    @Test
    fun `repository failures retain typed errors and causes`() = runTest {
        val failures = listOf(
            IOException("offline") to TriviaError.NETWORK,
            IllegalArgumentException("bad config") to TriviaError.INVALID_CONFIGURATION,
            IllegalStateException("unexpected") to TriviaError.UNKNOWN,
            TriviaSdkException(TriviaError.RATE_LIMIT) to TriviaError.RATE_LIMIT
        )
        for ((failure, error) in failures) {
            repository.failure = failure
            sdk.start()
            assertEquals(error, (sdk.state.value as TriviaState.Failed).failure.error)
        }
    }

    @Test
    fun `categories and language remain independent of an active game`() = runTest {
        val categories = listOf(TriviaCategory(9, "General Knowledge"))
        repository.categories = categories
        assertEquals(categories, sdk.loadCategories())
        sdk.start()
        val session = (sdk.state.value as TriviaState.Playing).session
        assertFalse(sdk.setContentLanguage(AppLanguage.ENGLISH))
        assertTrue(sdk.setContentLanguage(AppLanguage.GERMAN))
        assertEquals(AppLanguage.GERMAN, localization.contentLanguage.value)
        assertEquals(session, (sdk.state.value as TriviaState.Playing).session)
        localization.setTranslationUnavailable(true)
        assertTrue(sdk.translationUnavailable.value)
        repository.failure = IOException("offline")
        try {
            sdk.loadCategories()
            fail("Expected category failure")
        } catch (e: TriviaSdkException) {
            assertEquals(TriviaError.NETWORK, e.error)
        }
    }

    @Test
    fun `canceled load restores prior state without persisting`() = runTest {
        val gate = CompletableDeferred<Unit>()
        repository.beforeQuestions = { gate.await() }
        val load = launch { sdk.start() }
        runCurrent()
        assertEquals(TriviaState.Loading, sdk.state.value)
        load.cancelAndJoin()
        assertEquals(TriviaState.Idle, sdk.state.value)
        assertNull(sessions.saved)
        repository.beforeQuestions = {}
        sdk.start()
        assertTrue(sdk.state.value is TriviaState.Playing)
    }

    @Test
    fun `canceling restore and categories propagates cancellation`() = runTest {
        val gate = CompletableDeferred<Unit>()
        sessions.beforeRead = { gate.await() }
        val restore = launch { sdk.restore() }
        runCurrent()
        restore.cancelAndJoin()
        assertEquals(TriviaState.Idle, sdk.state.value)
        repository.beforeCategories = { gate.await() }
        val categories = launch { sdk.loadCategories() }
        runCurrent()
        categories.cancelAndJoin()
        assertTrue(categories.isCancelled)
    }

    @Test
    fun `finish waits for an in-flight save then clears so stale save cannot resurrect game`() = runTest {
        sdk.start()
        val gate = CompletableDeferred<Unit>()
        sessions.beforeWrite = { gate.await() }
        val answer = async { sdk.submit("Water") }
        runCurrent()
        val finish = async { sdk.finishEarly() }
        runCurrent()
        assertFalse(finish.isCompleted)
        sessions.beforeWrite = {}
        gate.complete(Unit)
        answer.await()
        finish.await()
        assertNull(sessions.saved)
        assertTrue(sdk.state.value is TriviaState.Finished)
    }

    @Test
    fun `committed save survives caller cancellation and releases the lock afterward`() = runTest {
        val gate = CompletableDeferred<Unit>()
        sessions.beforeWrite = { gate.await() }
        val start = launch { sdk.start() }
        runCurrent()
        start.cancel()
        assertFalse(start.isCompleted)
        sessions.beforeWrite = {}
        gate.complete(Unit)
        start.join()
        assertTrue(start.isCancelled)
        assertTrue(sdk.state.value is TriviaState.Playing)
        assertTrue(sessions.saved != null)
        sdk.reset()
        assertNull(sessions.saved)
    }

    private class FakeClock : GameClock {
        var wall = 1_790_000_000_000L
        var elapsed = 0L
        override fun wallTimeMillis(): Long = wall
        override fun elapsedTimeMillis(): Long = elapsed
        fun advance(millis: Long) {
            wall += millis
            elapsed += millis
        }
    }

    private class MemoryTriviaRepository(var questions: List<TriviaQuestion>) : ITriviaRepository {
        var categories = emptyList<TriviaCategory>()
        var questionRequests = 0
        var failure: Exception? = null
        var beforeQuestions: suspend () -> Unit = {}
        var beforeCategories: suspend () -> Unit = {}
        override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> {
            questionRequests++
            beforeQuestions()
            failure?.let { throw it }
            return questions
        }
        override suspend fun getCategories(): List<TriviaCategory> {
            beforeCategories()
            failure?.let { throw it }
            return categories
        }
    }

    private class MemorySessions : IGameSessionRepository {
        var saved: GameSessionState? = null
        var readFailure: Exception? = null
        var writeFailure: Exception? = null
        var beforeRead: suspend () -> Unit = {}
        var beforeWrite: suspend () -> Unit = {}
        override suspend fun saveGameSession(
            questions: List<TriviaQuestion>, currentIndex: Int, correctCount: Int,
            activeElapsedMillis: Long, selectedAnswers: Map<Int, String>
        ) {
            beforeWrite()
            writeFailure?.let { throw it }
            saved = GameSessionState(questions, currentIndex, correctCount, activeElapsedMillis, selectedAnswers)
        }
        override fun getActiveSessionFlow() = MutableStateFlow(saved)
        override suspend fun getActiveSession(): GameSessionState? {
            beforeRead()
            readFailure?.let { throw it }
            return saved
        }
        override suspend fun clearActiveSession() {
            beforeWrite()
            writeFailure?.let { throw it }
            saved = null
        }
    }

    private class MemoryLocalization : IContentLocalizationRepository {
        override val contentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
        override val translationUnavailable = MutableStateFlow(false)
        override fun setContentLanguage(language: AppLanguage) { contentLanguage.value = language }
        override fun setTranslationUnavailable(unavailable: Boolean) { translationUnavailable.value = unavailable }
    }
}
