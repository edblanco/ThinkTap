package com.dosparta.trivia.ui.preview

import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText

internal object PreviewFixtures {
    val question = TriviaQuestion(
        category = "Science",
        type = "multiple",
        difficulty = "easy",
        question = "Which planet is known as the Red Planet?",
        correctAnswer = "Mars",
        options = listOf("Earth", "Mars", "Venus", "Jupiter")
    )
    val session = GameSession(questions = listOf(question, question), startTimeMillis = 0L)
    val booleanQuestion = TriviaQuestion(
        category = "Ciencia y naturaleza",
        type = "boolean",
        difficulty = "medium",
        question = "¿El agua hierve a 100 °C al nivel del mar?",
        correctAnswer = "True",
        options = listOf("True", "False")
    )
    val booleanSession = GameSession(questions = listOf(booleanQuestion), startTimeMillis = 0L)
    val categories = listOf(
        TriviaCategory(id = 9, name = "General Knowledge"),
        TriviaCategory(id = 17, name = "Science & Nature")
    )
    val result = GameResult(totalQuestions = 10, correctAnswers = 7, durationMillis = 125_000L)
    val error = UiText.StringResource(R.string.error_network_occurred)
}
