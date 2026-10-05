package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession

internal class FinishGameUseCase(
    private val engine: GameEngine
) {
    operator fun invoke(session: GameSession): GameResult =
        engine.finish(session)
}
