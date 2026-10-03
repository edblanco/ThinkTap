package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.repository.IGameSessionRepository
import javax.inject.Inject

internal class ClearGameSessionUseCase @Inject constructor(
    private val gameSessionRepository: IGameSessionRepository
) {
    suspend operator fun invoke() {
        gameSessionRepository.clearActiveSession()
    }
}
