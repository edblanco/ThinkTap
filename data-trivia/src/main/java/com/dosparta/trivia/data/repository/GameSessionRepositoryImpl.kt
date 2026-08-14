package com.dosparta.trivia.data.repository

import com.dosparta.trivia.data.local.dao.GameSessionDao
import com.dosparta.trivia.data.mapper.GameSessionMapper
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class GameSessionRepositoryImpl @Inject constructor(
    private val gameSessionDao: GameSessionDao
) : IGameSessionRepository {

    override suspend fun saveGameSession(
        questions: List<TriviaQuestion>,
        currentIndex: Int,
        correctCount: Int,
        activeElapsedMillis: Long,
        selectedAnswers: Map<Int, String>
    ) {
        val entity = GameSessionMapper.toEntity(
            questions = questions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = selectedAnswers
        )
        gameSessionDao.insertOrReplaceSession(entity)
    }

    override fun getActiveSessionFlow(): Flow<GameSessionState?> {
        return gameSessionDao.getActiveSession().map { entity ->
            entity?.let { GameSessionMapper.fromEntity(it) }
        }
    }

    override suspend fun getActiveSession(): GameSessionState? {
        return gameSessionDao.getActiveSessionOnce()?.let { entity ->
            GameSessionMapper.fromEntity(entity)
        }
    }

    override suspend fun clearActiveSession() {
        gameSessionDao.clearActiveSession()
    }
}
