package com.dosparta.triviagame2.reminder

import org.junit.Assert.assertEquals
import org.junit.After
import org.junit.Before
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class DailyQuizReminderSchedulerTest {
    private lateinit var originalTimeZone: TimeZone

    @Before
    fun setUp() {
        originalTimeZone = TimeZone.getDefault()
        TimeZone.setDefault(TimeZone.getTimeZone("UTC"))
    }

    @After
    fun tearDown() {
        TimeZone.setDefault(originalTimeZone)
    }

    @Test
    fun `calculateInitialDelayMillis returns same-day delay when target time is ahead`() {
        val nowMillis = utcMillis(2026, Calendar.JANUARY, 1, 10, 15, 0)

        val delay = DailyQuizReminderScheduler.calculateInitialDelayMillis(
            hour = 19,
            minute = 0,
            nowMillis = nowMillis
        )

        assertEquals((8 * 60 + 45) * 60 * 1000L, delay)
    }

    @Test
    fun `calculateInitialDelayMillis returns next-day delay when target time passed`() {
        val nowMillis = utcMillis(2026, Calendar.JANUARY, 1, 20, 0, 0)

        val delay = DailyQuizReminderScheduler.calculateInitialDelayMillis(
            hour = 19,
            minute = 0,
            nowMillis = nowMillis
        )

        assertEquals(23 * 60 * 60 * 1000L, delay)
    }

    private fun utcMillis(
        year: Int,
        month: Int,
        day: Int,
        hour: Int,
        minute: Int,
        second: Int
    ): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            set(Calendar.YEAR, year)
            set(Calendar.MONTH, month)
            set(Calendar.DAY_OF_MONTH, day)
            set(Calendar.HOUR_OF_DAY, hour)
            set(Calendar.MINUTE, minute)
            set(Calendar.SECOND, second)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }
    }
}
