package com.dosparta.trivia.domain.repository

import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.flow.Flow

data class GameSessionState(
    val questions: List<TriviaQuestion>,
    val currentIndex: Int,
    val correctCount: Int,
    val activeElapsedMillis: Long,
    val selectedAnswers: Map<Int, String>
)

interface IGameSessionRepository {
    suspend fun saveGameSession(
        questions: List<TriviaQuestion>,
        currentIndex: Int,
        correctCount: Int,
        activeElapsedMillis: Long,
        selectedAnswers: Map<Int, String>
    )

    fun getActiveSessionFlow(): Flow<GameSessionState?>

    suspend fun getActiveSession(): GameSessionState?

    suspend fun clearActiveSession()
}
