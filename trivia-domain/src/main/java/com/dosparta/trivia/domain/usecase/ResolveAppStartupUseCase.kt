package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.StartupDecision
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import javax.inject.Inject

class ResolveAppStartupUseCase @Inject constructor(
    private val gameSessionRepository: IGameSessionRepository
) {
    suspend operator fun invoke(): StartupDecision {
        val activeSession = gameSessionRepository.getActiveSession()
        return activeSession
            ?.takeIf { it.questions.isNotEmpty() }
            ?.let(StartupDecision::RestoreGame)
            ?: StartupDecision.LoadCategories
    }
}
