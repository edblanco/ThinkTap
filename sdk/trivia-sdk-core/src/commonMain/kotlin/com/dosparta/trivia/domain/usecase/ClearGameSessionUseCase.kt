package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.repository.IGameSessionRepository

internal class ClearGameSessionUseCase(
    private val gameSessionRepository: IGameSessionRepository
) {
    suspend operator fun invoke() {
        gameSessionRepository.clearActiveSession()
    }
}
