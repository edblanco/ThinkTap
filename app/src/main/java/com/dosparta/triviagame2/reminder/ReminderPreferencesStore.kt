package com.dosparta.triviagame2.reminder

import android.content.Context

class ReminderPreferencesStore(
    context: Context
) {
    private val preferences = context.getSharedPreferences(PREFERENCES_NAME, Context.MODE_PRIVATE)

    fun load(): ReminderSettings {
        val enabled = preferences.getBoolean(KEY_ENABLED, false)
        val hour = preferences.getInt(KEY_HOUR, DEFAULT_HOUR)
        val minute = preferences.getInt(KEY_MINUTE, DEFAULT_MINUTE)
        return ReminderSettings(
            enabled = enabled,
            hour = hour.coerceIn(0, 23),
            minute = minute.coerceIn(0, 59)
        )
    }

    fun saveEnabled(enabled: Boolean) {
        preferences.edit()
            .putBoolean(KEY_ENABLED, enabled)
            .apply()
    }

    fun saveTime(hour: Int, minute: Int) {
        require(hour in 0..23) { "Hour must be between 0 and 23." }
        require(minute in 0..59) { "Minute must be between 0 and 59." }
        preferences.edit()
            .putInt(KEY_HOUR, hour)
            .putInt(KEY_MINUTE, minute)
            .apply()
    }

    companion object {
        const val DEFAULT_HOUR = 19
        const val DEFAULT_MINUTE = 0

        private const val PREFERENCES_NAME = "daily_reminder_preferences"
        private const val KEY_ENABLED = "enabled"
        private const val KEY_HOUR = "hour"
        private const val KEY_MINUTE = "minute"
    }
}
