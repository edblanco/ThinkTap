package com.dosparta.triviagame2.reminder

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build
import com.dosparta.triviagame2.R

object ReminderNotificationChannel {
    const val CHANNEL_ID = "daily_quiz_reminder"

    fun createIfNeeded(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(NotificationManager::class.java) ?: return
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.daily_reminder_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.daily_reminder_channel_description)
        }
        manager.createNotificationChannel(channel)
    }
}
