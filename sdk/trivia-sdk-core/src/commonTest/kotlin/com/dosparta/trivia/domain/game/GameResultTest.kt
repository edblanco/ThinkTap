package com.dosparta.trivia.domain.game

import kotlin.test.Test
import kotlin.test.assertEquals

class GameResultTest {

    @Test
    fun `formattedDuration returns 0 seconds as 0_00`() {
        val result = GameResult(
            totalQuestions = 1,
            correctAnswers = 1,
            durationMillis = 0L
        )
        assertEquals("0:00", result.formattedDuration())
    }

    @Test
    fun `formattedDuration returns seconds under one minute correctly`() {
        // 5 seconds = 5000 ms
        val result = GameResult(
            totalQuestions = 1,
            correctAnswers = 1,
            durationMillis = 5_000L
        )
        assertEquals("0:05", result.formattedDuration())
    }

    @Test
    fun `formattedDuration returns minutes and seconds correctly`() {
        // 1 minute 7 seconds = 67_000 ms
        val result = GameResult(
            totalQuestions = 1,
            correctAnswers = 1,
            durationMillis = 67_000L
        )
        assertEquals("1:07", result.formattedDuration())
    }

    @Test
    fun `formattedDuration handles multiple minutes`() {
        // 12 minutes 34 seconds = (12 * 60 + 34) * 1000 ms
        val result = GameResult(
            totalQuestions = 1,
            correctAnswers = 1,
            durationMillis = (12 * 60 + 34) * 1_000L
        )
        assertEquals("12:34", result.formattedDuration())
    }
}
