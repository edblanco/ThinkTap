package com.dosparta.trivia.domain.game

import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.sdk.GameClock

data class GameSession(
    val questions: List<TriviaQuestion>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val startTimeMillis: Long = GameClock.SYSTEM.wallTimeMillis()
) {
    val currentQuestion: TriviaQuestion?
        get() = questions.getOrNull(currentIndex)

    val hasMoreQuestions: Boolean
        get() = currentQuestion != null
}
