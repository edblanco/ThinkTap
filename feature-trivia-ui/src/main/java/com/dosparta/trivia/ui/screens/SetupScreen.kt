package com.dosparta.trivia.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import java.util.Locale
import kotlin.math.roundToInt

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SetupScreen(
    categories: List<TriviaCategory>,
    categoriesError: UiText?,
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onStartGame: (TriviaConfig) -> Unit,
    onRetryLoadCategories: () -> Unit,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    var selectedAmount by remember { mutableIntStateOf(10) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedDifficulty by remember { mutableStateOf("mixed") }
    var categoryExpanded by remember { mutableStateOf(false) }
    var difficultyExpanded by remember { mutableStateOf(false) }

    val selectedCategoryName = categories
        .firstOrNull { it.id == selectedCategoryId }
        ?.name
        ?: stringResource(R.string.any_category)

    if (categoriesError != null) {
        Scaffold(
            topBar = {
                TopAppBar(
                    title = { Text("Trivia Game") },
                    colors = TopAppBarDefaults.topAppBarColors(
                        containerColor = MaterialTheme.colorScheme.background,
                        titleContentColor = MaterialTheme.colorScheme.onBackground
                    )
                )
            }
        ) { _ ->
            ErrorScreen(
                message = categoriesError,
                onRetry = onRetryLoadCategories,
            )
        }
        return
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Trivia Game") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.background,
                    titleContentColor = MaterialTheme.colorScheme.onBackground
                )
            )
        }
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .padding(24.dp)
                .testTag("setup_screen")
        ) {
            Column(
                modifier = Modifier
                    .align(Alignment.Center)
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = stringResource(R.string.ready_to_play),
                    style = MaterialTheme.typography.headlineSmall,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                Text(
                    text = "${stringResource(R.string.question_amount)}: $selectedAmount",
                    style = MaterialTheme.typography.bodyLarge,
                    modifier = Modifier.fillMaxWidth()
                )

                Slider(
                    value = selectedAmount.toFloat(),
                    onValueChange = { selectedAmount = it.roundToInt() },
                    valueRange = 10f..50f,
                    steps = 39,
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("amount_slider")
                )

                Box {
                    OutlinedButton(
                        onClick = { categoryExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("category_button")
                    ) {
                        Text(text = selectedCategoryName)
                    }

                    DropdownMenu(
                        expanded = categoryExpanded,
                        onDismissRequest = { categoryExpanded = false }
                    ) {
                        DropdownMenuItem(
                            text = { Text(stringResource(R.string.any_category)) },
                            onClick = {
                                selectedCategoryId = null
                                categoryExpanded = false
                            }
                        )

                        categories.forEach { category ->
                            DropdownMenuItem(
                                text = { Text(category.name) },
                                onClick = {
                                    selectedCategoryId = category.id
                                    categoryExpanded = false
                                }
                            )
                        }
                    }
                }

                Box {
                    OutlinedButton(
                        onClick = { difficultyExpanded = true },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("difficulty_button")
                    ) {
                        Text(
                            if (selectedDifficulty == "mixed") "Mixed" else selectedDifficulty.replaceFirstChar { it.uppercase() }
                        )
                    }

                    DropdownMenu(
                        expanded = difficultyExpanded,
                        onDismissRequest = { difficultyExpanded = false }
                    ) {
                        listOf("mixed", "easy", "medium", "hard").forEach { difficulty ->
                            DropdownMenuItem(
                                text = { Text(difficulty.replaceFirstChar { it.uppercase() }) },
                                onClick = {
                                    selectedDifficulty = difficulty
                                    difficultyExpanded = false
                                }
                            )
                        }
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = stringResource(R.string.daily_reminder),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    Switch(
                        checked = reminderEnabled,
                        onCheckedChange = onReminderEnabledChange,
                        modifier = Modifier.testTag("daily_reminder_toggle")
                    )
                }

                if (reminderEnabled) {
                    OutlinedButton(
                        onClick = {
                            TimePickerDialog(
                                context,
                                { _, hourOfDay, minute ->
                                    onReminderTimeChange(hourOfDay, minute)
                                },
                                reminderHour,
                                reminderMinute,
                                true
                            ).show()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .testTag("daily_reminder_time_button")
                    ) {
                        Text(
                            text = stringResource(
                                R.string.daily_reminder_time,
                                String.format(Locale.getDefault(), "%02d:%02d", reminderHour, reminderMinute)
                            )
                        )
                    }
                }

                Button(
                    onClick = {
                        val config = TriviaConfig(
                            amount = selectedAmount,
                            categoryId = selectedCategoryId,
                            difficulty = selectedDifficulty
                        )
                        onStartGame(config)
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_game_button")
                ) {
                    Text(text = stringResource(R.string.start_game))
                }
            }
        }
    }
}
