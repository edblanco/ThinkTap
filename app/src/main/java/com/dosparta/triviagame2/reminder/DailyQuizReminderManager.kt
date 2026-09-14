package com.dosparta.triviagame2.reminder

import android.content.Context

class DailyQuizReminderManager(
    context: Context
) {
    private val preferencesStore = ReminderPreferencesStore(context)
    private val scheduler = DailyQuizReminderScheduler(context)

    fun getSettings(): ReminderSettings = preferencesStore.load()

    fun setEnabled(enabled: Boolean): ReminderSettings {
        preferencesStore.saveEnabled(enabled)
        val settings = preferencesStore.load()
        scheduler.rescheduleFromSettings(settings)
        return settings
    }

    fun setTime(hour: Int, minute: Int): ReminderSettings {
        preferencesStore.saveTime(hour, minute)
        val settings = preferencesStore.load()
        if (settings.enabled) {
            scheduler.scheduleDaily(settings.hour, settings.minute)
        }
        return settings
    }

    fun rescheduleFromPreferences() {
        scheduler.rescheduleFromSettings(preferencesStore.load())
    }
}
