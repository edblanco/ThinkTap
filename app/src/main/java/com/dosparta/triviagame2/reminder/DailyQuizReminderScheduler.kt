package com.dosparta.triviagame2.reminder

import android.content.Context
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import java.util.Calendar
import java.util.concurrent.TimeUnit

class DailyQuizReminderScheduler(
    context: Context
) {
    private val workManager = WorkManager.getInstance(context)

    fun scheduleDaily(hour: Int, minute: Int) {
        require(hour in MIN_HOUR..MAX_HOUR) { "Hour must be between 0 and 23." }
        require(minute in MIN_MINUTE..MAX_MINUTE) { "Minute must be between 0 and 59." }

        val request = PeriodicWorkRequestBuilder<DailyQuizReminderWorker>(
            repeatInterval = REPEAT_INTERVAL_HOURS,
            repeatIntervalTimeUnit = TimeUnit.HOURS,
            flexTimeInterval = FLEX_INTERVAL_HOURS,
            flexTimeIntervalUnit = TimeUnit.HOURS
        )
            .setInitialDelay(calculateInitialDelayMillis(hour, minute), TimeUnit.MILLISECONDS)
            .build()

        workManager.enqueueUniquePeriodicWork(
            WORK_NAME,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun cancelDaily() {
        workManager.cancelUniqueWork(WORK_NAME)
    }

    fun rescheduleFromSettings(settings: ReminderSettings) {
        if (settings.enabled) {
            scheduleDaily(settings.hour, settings.minute)
        } else {
            cancelDaily()
        }
    }

    companion object {
        const val WORK_NAME = "daily-quiz-reminder-work"
        private const val MIN_HOUR = 0
        private const val MAX_HOUR = 23
        private const val MIN_MINUTE = 0
        private const val MAX_MINUTE = 59
        private const val REPEAT_INTERVAL_HOURS = 24L
        private const val FLEX_INTERVAL_HOURS = 1L

        internal fun calculateInitialDelayMillis(
            hour: Int,
            minute: Int,
            nowMillis: Long = System.currentTimeMillis()
        ): Long {
            val now = Calendar.getInstance().apply {
                timeInMillis = nowMillis
            }
            val nextRun = Calendar.getInstance().apply {
                timeInMillis = nowMillis
                set(Calendar.HOUR_OF_DAY, hour)
                set(Calendar.MINUTE, minute)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
                if (timeInMillis <= now.timeInMillis) {
                    add(Calendar.DAY_OF_YEAR, 1)
                }
            }
            return (nextRun.timeInMillis - now.timeInMillis).coerceAtLeast(0L)
        }
    }
}
