package com.dosparta.trivia.domain.game

/**
 * Represents the outcome of a finished trivia session.
 *
 * @param totalQuestions how many questions were in the session
 * @param correctAnswers how many of those were answered correctly
 * @param durationMillis how long the player took from start to finish
 */
data class GameResult(
    val totalQuestions: Int,
    val correctAnswers: Int,
    val durationMillis: Long
) {
    /** How many were answered incorrectly */
    val incorrectAnswers: Int
        get() = totalQuestions - correctAnswers

    /** The percentage score (0.0–100.0) */
    val scorePercentage: Double
        get() = if (totalQuestions > 0)
            correctAnswers.toDouble() / totalQuestions * 100
        else
            0.0

    /** A human-readable duration string that handles longer sessions clearly and consistently. */
    fun formattedDuration(): String {
        val totalSeconds = kotlin.math.max(0L, durationMillis / 1_000)
        val hours = totalSeconds / 3_600
        val minutes = (totalSeconds % 3_600) / 60
        val seconds = totalSeconds % 60

        return if (hours > 0) {
            "%d:%02d:%02d".format(hours, minutes, seconds)
        } else {
            "%d:%02d".format(minutes, seconds)
        }
    }
}