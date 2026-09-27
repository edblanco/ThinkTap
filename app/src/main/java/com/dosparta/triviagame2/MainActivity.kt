package com.dosparta.triviagame2

import android.Manifest
import android.content.pm.PackageManager
import android.os.Bundle
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.ContextCompat
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.dosparta.trivia.ui.navigation.TriviaNavHost
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import com.dosparta.triviagame2.reminder.DailyQuizReminderManager
import com.dosparta.triviagame2.reminder.ReminderSettings
import com.dosparta.core.ui.theme.TriviaGame2Theme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val viewModel: TriviaViewModel by viewModels()
    private val reminderManager by lazy { DailyQuizReminderManager(applicationContext) }
    private var reminderSettings by mutableStateOf(ReminderSettings())

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            reminderSettings = reminderManager.setEnabled(true)
        } else {
            reminderSettings = reminderManager.setEnabled(false)
            Toast.makeText(
                this,
                getString(R.string.notification_permission_denied),
                Toast.LENGTH_LONG
            ).show()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        reminderSettings = reminderManager.getSettings()
        reminderManager.rescheduleFromPreferences()

        lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onPause(owner: LifecycleOwner) {
                viewModel.saveGameOnPause()
            }

            override fun onResume(owner: LifecycleOwner) {
                viewModel.onAppResumed()
            }
        })

        setContent {
            TriviaGame2Theme {
                TriviaNavHost(
                    reminderEnabled = reminderSettings.enabled,
                    reminderHour = reminderSettings.hour,
                    reminderMinute = reminderSettings.minute,
                    onReminderEnabledChange = { enabled ->
                        if (!enabled) {
                            reminderSettings = reminderManager.setEnabled(false)
                        } else {
                            if (canPostNotifications()) {
                                reminderSettings = reminderManager.setEnabled(true)
                            } else {
                                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            }
                        }
                    },
                    onReminderTimeChange = { hour, minute ->
                        reminderSettings = reminderManager.setTime(hour, minute)
                    }
                )
            }
        }
    }

    private fun canPostNotifications(): Boolean {
        return ContextCompat.checkSelfPermission(
            this,
            Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }
}
