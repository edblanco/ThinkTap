package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import javax.inject.Inject

internal class FinishGameUseCase @Inject constructor(
    private val engine: GameEngine
) {
    operator fun invoke(session: GameSession): GameResult =
        engine.finish(session)
}
