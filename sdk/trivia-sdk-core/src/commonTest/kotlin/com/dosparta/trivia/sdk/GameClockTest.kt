package com.dosparta.trivia.sdk

import com.dosparta.trivia.domain.game.GameSession
import kotlin.test.Test
import kotlin.test.assertTrue

class GameClockTest {
    @Test
    fun systemClockProvidesEpochTimestampsAndMonotonicDurations() {
        val clock = GameClock.SYSTEM
        val before = clock.wallTimeMillis()
        val session = GameSession(emptyList())
        val after = clock.wallTimeMillis()
        assertTrue(before > 0L)
        assertTrue(session.startTimeMillis in before..after)
        val elapsed = clock.elapsedTimeMillis()
        assertTrue(clock.elapsedTimeMillis() >= elapsed)
    }
}
