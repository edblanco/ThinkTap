package com.dosparta.triviagame2.reminder

import android.Manifest
import android.app.PendingIntent
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.dosparta.triviagame2.MainActivity
import com.dosparta.triviagame2.R

class DailyQuizReminderWorker(
    appContext: android.content.Context,
    workerParameters: WorkerParameters
) : CoroutineWorker(appContext, workerParameters) {

    override suspend fun doWork(): Result {
        if (!canPostNotifications()) {
            return Result.success()
        }

        ReminderNotificationChannel.createIfNeeded(applicationContext)

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            REQUEST_CODE_OPEN_APP,
            Intent(applicationContext, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(applicationContext, ReminderNotificationChannel.CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_trivia_launcher)
            .setContentTitle(applicationContext.getString(R.string.daily_reminder_title))
            .setContentText(applicationContext.getString(R.string.daily_reminder_body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        NotificationManagerCompat.from(applicationContext).notify(NOTIFICATION_ID, notification)
        return Result.success()
    }

    private fun canPostNotifications(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ActivityCompat.checkSelfPermission(
            applicationContext,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    private companion object {
        const val NOTIFICATION_ID = 1001
        const val REQUEST_CODE_OPEN_APP = 1002
    }
}
