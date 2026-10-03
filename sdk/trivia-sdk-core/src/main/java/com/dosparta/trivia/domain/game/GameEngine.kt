package com.dosparta.trivia.domain.game

import javax.inject.Inject

internal class GameEngine @Inject constructor() {
    fun submitAnswer(session: GameSession, answer: String): GameSession {
        val currentQuestion = session.currentQuestion ?: return session
        val normalizedAnswer = normalizeAnswer(answer)
        val normalizedCorrectAnswer = normalizeAnswer(currentQuestion.correctAnswer)
        val isCorrect = normalizedAnswer == normalizedCorrectAnswer
        return session.copy(
            currentIndex = session.currentIndex + 1,
            correctCount = session.correctCount + if (isCorrect) 1 else 0
        )
    }

    private fun normalizeAnswer(value: String): String =
        value.trim().lowercase().replace(Regex("\\s+"), " ")

    fun hasMoreQuestions(session: GameSession): Boolean =
        session.currentIndex < session.questions.size

    fun finish(session: GameSession): GameResult {
        val elapsed = System.currentTimeMillis() - session.startTimeMillis
        return GameResult(
            totalQuestions = session.questions.size,
            correctAnswers = session.correctCount,
            durationMillis   = elapsed
        )
    }
}
