package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import javax.inject.Inject

class PersistGameSessionUseCase @Inject constructor(
    private val gameSessionRepository: IGameSessionRepository
) {
    suspend operator fun invoke(
        questions: List<TriviaQuestion>,
        currentIndex: Int,
        correctCount: Int,
        activeElapsedMillis: Long,
        selectedAnswers: Map<Int, String>
    ) {
        gameSessionRepository.saveGameSession(
            questions = questions,
            currentIndex = currentIndex,
            correctCount = correctCount,
            activeElapsedMillis = activeElapsedMillis,
            selectedAnswers = selectedAnswers
        )
    }
}
