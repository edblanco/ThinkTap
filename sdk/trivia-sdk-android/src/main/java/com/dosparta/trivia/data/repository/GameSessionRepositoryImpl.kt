package com.dosparta.trivia.data.repository

import com.dosparta.trivia.data.local.dao.GameSessionDao
import com.dosparta.trivia.data.mapper.GameSessionMapper
import com.dosparta.trivia.data.remote.persistenceCall
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GameSessionRepositoryImpl @Inject constructor(
    private val gameSessionDao: GameSessionDao,
    private val ioDispatcher: CoroutineDispatcher = Dispatchers.IO
) : IGameSessionRepository {

    override suspend fun saveGameSession(
        questions: List<TriviaQuestion>,
        currentIndex: Int,
        correctCount: Int,
        activeElapsedMillis: Long,
        selectedAnswers: Map<Int, String>
    ) = withContext(ioDispatcher) {
        persistenceCall {
            val entity = GameSessionMapper.toEntity(
                questions = questions,
                currentIndex = currentIndex,
                correctCount = correctCount,
                activeElapsedMillis = activeElapsedMillis,
                selectedAnswers = selectedAnswers
            )
            gameSessionDao.insertOrReplaceSession(entity)
        }
    }

    override fun getActiveSessionFlow(): Flow<GameSessionState?> {
        return persistenceCall { gameSessionDao.getActiveSession() }.map { entity ->
            persistenceCall { entity?.let { GameSessionMapper.fromEntity(it) } }
        }.catch { error ->
            if (error is CancellationException || error is TriviaSdkException) throw error
            throw TriviaSdkException(TriviaError.PERSISTENCE, error)
        }.flowOn(ioDispatcher)
    }

    override suspend fun getActiveSession(): GameSessionState? = withContext(ioDispatcher) {
        persistenceCall {
            gameSessionDao.getActiveSessionOnce()?.let { entity ->
                GameSessionMapper.fromEntity(entity)
            }
        }
    }

    override suspend fun clearActiveSession() = withContext(ioDispatcher) {
        persistenceCall { gameSessionDao.clearActiveSession() }
    }
}
