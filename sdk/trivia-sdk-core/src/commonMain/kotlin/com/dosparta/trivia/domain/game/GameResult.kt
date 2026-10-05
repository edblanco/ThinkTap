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
            correctAnswers.toDouble() / totalQuestions * PERCENT_SCALE
        else
            0.0

    /** A human-readable duration string that handles longer sessions clearly and consistently. */
    fun formattedDuration(): String {
        val totalSeconds = kotlin.math.max(0L, durationMillis / MILLIS_IN_SECOND)
        val hours = totalSeconds / SECONDS_IN_HOUR
        val minutes = (totalSeconds % SECONDS_IN_HOUR) / SECONDS_IN_MINUTE
        val seconds = totalSeconds % SECONDS_IN_MINUTE
        val paddedMinutes = minutes.toString().padStart(2, '0')
        val paddedSeconds = seconds.toString().padStart(2, '0')

        return if (hours > 0) {
            "$hours:$paddedMinutes:$paddedSeconds"
        } else {
            "$minutes:$paddedSeconds"
        }
    }

    private companion object {
        const val PERCENT_SCALE = 100
        const val MILLIS_IN_SECOND = 1_000L
        const val SECONDS_IN_MINUTE = 60L
        const val SECONDS_IN_HOUR = 3_600L
    }
}
