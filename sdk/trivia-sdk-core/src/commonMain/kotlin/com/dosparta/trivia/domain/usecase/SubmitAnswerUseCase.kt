package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameSession

internal class SubmitAnswerUseCase(
    private val engine: GameEngine
) {
    operator fun invoke(session: GameSession, answer: String): GameSession =
        engine.submitAnswer(session, answer)
}
