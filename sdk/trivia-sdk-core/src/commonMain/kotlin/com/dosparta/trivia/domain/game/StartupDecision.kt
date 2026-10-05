package com.dosparta.trivia.domain.game

import com.dosparta.trivia.domain.repository.GameSessionState

internal sealed interface StartupDecision {
    data object LoadCategories : StartupDecision
    data class RestoreGame(val session: GameSessionState) : StartupDecision
}
