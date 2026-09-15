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
        val nowMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 10)
            set(Calendar.MINUTE, 15)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

        val delay = DailyQuizReminderScheduler.calculateInitialDelayMillis(
            hour = 19,
            minute = 0,
            nowMillis = nowMillis
        )

        assertEquals((8 * 60 + 45) * 60 * 1000L, delay)
    }

    @Test
    fun `calculateInitialDelayMillis returns next-day delay when target time passed`() {
        val nowMillis = Calendar.getInstance(TimeZone.getTimeZone("UTC")).run {
            set(Calendar.YEAR, 2026)
            set(Calendar.MONTH, Calendar.JANUARY)
            set(Calendar.DAY_OF_MONTH, 1)
            set(Calendar.HOUR_OF_DAY, 20)
            set(Calendar.MINUTE, 0)
            set(Calendar.SECOND, 0)
            set(Calendar.MILLISECOND, 0)
            timeInMillis
        }

        val delay = DailyQuizReminderScheduler.calculateInitialDelayMillis(
            hour = 19,
            minute = 0,
            nowMillis = nowMillis
        )

        assertEquals(23 * 60 * 60 * 1000L, delay)
    }
}
