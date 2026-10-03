package com.dosparta.trivia.sdk

import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession

sealed interface TriviaState {
    data object Idle : TriviaState
    data object Loading : TriviaState
    data class Playing(val session: GameSession) : TriviaState
    data class Finished(val result: GameResult) : TriviaState
    data class Failed(val failure: TriviaSdkException) : TriviaState
}

enum class TriviaError {
    INVALID_CONFIGURATION,
    NO_QUESTIONS,
    NO_RESULTS,
    INVALID_QUERY,
    RATE_LIMIT,
    NETWORK,
    PROTOCOL,
    PERSISTENCE,
    UNKNOWN
}

class TriviaSdkException(
    val error: TriviaError,
    cause: Throwable? = null
) : IllegalStateException(error.name, cause)
