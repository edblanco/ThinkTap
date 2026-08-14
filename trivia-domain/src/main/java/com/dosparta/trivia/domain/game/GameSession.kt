package com.dosparta.trivia.domain.game

import com.dosparta.trivia.domain.model.TriviaQuestion

data class GameSession(
    val questions: List<TriviaQuestion>,
    val currentIndex: Int = 0,
    val correctCount: Int = 0,
    val startTimeMillis: Long = System.currentTimeMillis()
) {
    val currentQuestion: TriviaQuestion?
        get() = questions.getOrNull(currentIndex)

    val hasMoreQuestions: Boolean
        get() = currentQuestion != null
}
