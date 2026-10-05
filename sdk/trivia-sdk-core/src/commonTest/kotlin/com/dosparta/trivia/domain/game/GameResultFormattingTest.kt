package com.dosparta.trivia.domain.game

import kotlin.test.Test
import kotlin.test.assertEquals

class GameResultFormattingTest {
    @Test
    fun durationFormattingUsesStableDigitsAndPadding() {
        val durations = mapOf(
            -1_000L to "0:00",
            0L to "0:00",
            9_999L to "0:09",
            65_000L to "1:05",
            3_599_000L to "59:59",
            3_600_000L to "1:00:00",
            3_725_000L to "1:02:05"
        )
        for ((duration, formatted) in durations) {
            assertEquals(formatted, GameResult(2, 1, duration).formattedDuration())
        }
    }

    @Test
    fun emptyGameHasZeroScore() {
        assertEquals(0.0, GameResult(0, 0, 0).scorePercentage)
    }
}
