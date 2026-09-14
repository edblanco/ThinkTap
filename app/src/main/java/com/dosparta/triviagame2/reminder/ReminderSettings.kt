package com.dosparta.triviagame2.reminder

data class ReminderSettings(
    val enabled: Boolean = false,
    val hour: Int = ReminderPreferencesStore.DEFAULT_HOUR,
    val minute: Int = ReminderPreferencesStore.DEFAULT_MINUTE
)
