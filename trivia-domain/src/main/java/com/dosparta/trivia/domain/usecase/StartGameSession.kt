package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.repository.ITriviaRepository
import javax.inject.Inject

class StartGameSession @Inject constructor(
    private val repo: ITriviaRepository
) {
    suspend operator fun invoke(config: TriviaConfig): GameSession {
        val questions = repo.getQuestions(config)
        require(questions.isNotEmpty()) { "No questions available for the requested game." }
        return GameSession(questions)
    }
}