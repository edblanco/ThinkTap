package com.dosparta.trivia.ui.screens

import android.app.TimePickerDialog
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import com.dosparta.core.ui.components.AnimatedCounter
import com.dosparta.core.ui.components.EnterAnimated
import com.dosparta.core.ui.components.TriviaCard
import com.dosparta.core.ui.components.TriviaDropdownField
import com.dosparta.core.ui.components.TriviaPrimaryButton
import com.dosparta.core.ui.components.TriviaScreenScaffold
import com.dosparta.core.ui.components.TriviaSecondaryButton
import com.dosparta.core.ui.components.TriviaSwitchRow
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme
import kotlin.math.roundToInt

private const val MIN_QUESTIONS = 10
private const val MAX_QUESTIONS = 50
private val DIFFICULTIES = listOf("mixed", "easy", "medium", "hard")

/**
 * The pre-game screen where the player configures a quiz and manages the daily reminder.
 */
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
    if (categoriesError != null) {
        ErrorScreen(message = categoriesError, onRetry = onRetryLoadCategories)
        return
    }

    var selectedAmount by remember { mutableIntStateOf(MIN_QUESTIONS) }
    var selectedCategoryId by remember { mutableStateOf<Int?>(null) }
    var selectedDifficulty by remember { mutableStateOf(DIFFICULTIES.first()) }

    TriviaScreenScaffold(title = stringResource(R.string.app_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("setup_screen")
                .verticalScroll(rememberScrollState())
                .padding(horizontal = 24.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            EnterAnimated(
                modifier = Modifier.fillMaxWidth(),
                index = 0
            ) {
                Text(
                    text = stringResource(R.string.ready_to_play),
                    style = MaterialTheme.typography.headlineMedium,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 1) {
                AmountCard(
                    amount = selectedAmount,
                    onAmountChange = { selectedAmount = it }
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 2) {
                CategoryField(
                    categories = categories,
                    selectedCategoryId = selectedCategoryId,
                    onCategorySelected = { selectedCategoryId = it }
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 3) {
                DifficultyField(
                    selectedDifficulty = selectedDifficulty,
                    onDifficultySelected = { selectedDifficulty = it }
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 4) {
                ReminderCard(
                    reminderEnabled = reminderEnabled,
                    reminderHour = reminderHour,
                    reminderMinute = reminderMinute,
                    onReminderEnabledChange = onReminderEnabledChange,
                    onReminderTimeChange = onReminderTimeChange
                )
            }

            EnterAnimated(modifier = Modifier.fillMaxWidth(), index = 5) {
                TriviaPrimaryButton(
                    text = stringResource(R.string.start_game),
                    onClick = {
                        onStartGame(
                            TriviaConfig(
                                amount = selectedAmount,
                                categoryId = selectedCategoryId,
                                difficulty = selectedDifficulty
                            )
                        )
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("start_game_button")
                )
            }
        }
    }
}

@Composable
private fun AmountCard(amount: Int, onAmountChange: (Int) -> Unit) {
    TriviaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = stringResource(R.string.question_amount),
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            AnimatedCounter(
                value = amount,
                contentDescription = stringResource(R.string.question_amount) + " $amount",
                style = MaterialTheme.typography.displaySmall,
                color = MaterialTheme.colorScheme.primary
            )
            Slider(
                value = amount.toFloat(),
                onValueChange = { onAmountChange(it.roundToInt()) },
                valueRange = MIN_QUESTIONS.toFloat()..MAX_QUESTIONS.toFloat(),
                steps = MAX_QUESTIONS - MIN_QUESTIONS - 1,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("amount_slider")
            )
        }
    }
}

@Composable
private fun CategoryField(
    categories: List<TriviaCategory>,
    selectedCategoryId: Int?,
    onCategorySelected: (Int?) -> Unit
) {
    val anyCategory = stringResource(R.string.any_category)
    val options = remember(categories, anyCategory) {
        listOf(anyCategory) + categories.map { it.name }
    }
    val selectedName = categories.firstOrNull { it.id == selectedCategoryId }?.name ?: anyCategory

    TriviaDropdownField(
        label = stringResource(R.string.category_label),
        value = selectedName,
        options = options,
        onOptionSelected = { index ->
            onCategorySelected(if (index == 0) null else categories[index - 1].id)
        },
        modifier = Modifier.testTag("category_button")
    )
}

@Composable
private fun DifficultyField(
    selectedDifficulty: String,
    onDifficultySelected: (String) -> Unit
) {
    val labels = listOf(
        stringResource(R.string.difficulty_mixed),
        stringResource(R.string.difficulty_easy),
        stringResource(R.string.difficulty_medium),
        stringResource(R.string.difficulty_hard)
    )
    val selectedLabel = labels[DIFFICULTIES.indexOf(selectedDifficulty).coerceAtLeast(0)]

    TriviaDropdownField(
        label = stringResource(R.string.difficulty_label),
        value = selectedLabel,
        options = labels,
        onOptionSelected = { index -> onDifficultySelected(DIFFICULTIES[index]) },
        modifier = Modifier.testTag("difficulty_button")
    )
}

@Composable
private fun ReminderCard(
    reminderEnabled: Boolean,
    reminderHour: Int,
    reminderMinute: Int,
    onReminderEnabledChange: (Boolean) -> Unit,
    onReminderTimeChange: (Int, Int) -> Unit
) {
    val context = LocalContext.current
    TriviaCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            TriviaSwitchRow(
                label = stringResource(R.string.daily_reminder),
                supportingText = stringResource(R.string.daily_reminder_supporting),
                checked = reminderEnabled,
                onCheckedChange = onReminderEnabledChange,
                switchModifier = Modifier.testTag("daily_reminder_toggle")
            )

            if (reminderEnabled) {
                TriviaSecondaryButton(
                    text = stringResource(
                        R.string.daily_reminder_time,
                        formatReminderTime(reminderHour, reminderMinute)
                    ),
                    onClick = {
                        TimePickerDialog(
                            context,
                            { _, hourOfDay, minute -> onReminderTimeChange(hourOfDay, minute) },
                            reminderHour,
                            reminderMinute,
                            true
                        ).show()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .testTag("daily_reminder_time_button")
                )
            }
        }
    }
}

/**
 * Formats a 24-hour time as `HH:mm` using ASCII digits.
 *
 * Padding is done manually instead of via [String.format] so the result never depends on the
 * ambient locale, which cannot be read in an observable way from a composable.
 */
private fun formatReminderTime(hour: Int, minute: Int): String =
    "${hour.toString().padStart(2, '0')}:${minute.toString().padStart(2, '0')}"

@PreviewLightDark
@Composable
private fun SetupScreenPreview() {
    TriviaPreviewTheme {
        SetupScreen(
            categories = PreviewFixtures.categories,
            categoriesError = null,
            reminderEnabled = false,
            reminderHour = 19,
            reminderMinute = 0,
            onStartGame = {},
            onRetryLoadCategories = {},
            onReminderEnabledChange = {},
            onReminderTimeChange = { _, _ -> }
        )
    }
}

@PreviewLightDark
@Composable
private fun SetupReminderEnabledPreview() {
    TriviaPreviewTheme {
        SetupScreen(
            categories = PreviewFixtures.categories,
            categoriesError = null,
            reminderEnabled = true,
            reminderHour = 19,
            reminderMinute = 30,
            onStartGame = {},
            onRetryLoadCategories = {},
            onReminderEnabledChange = {},
            onReminderTimeChange = { _, _ -> }
        )
    }
}

@PreviewLightDark
@Composable
private fun SetupErrorPreview() {
    TriviaPreviewTheme {
        SetupScreen(
            categories = emptyList(),
            categoriesError = PreviewFixtures.error,
            reminderEnabled = false,
            reminderHour = 19,
            reminderMinute = 0,
            onStartGame = {},
            onRetryLoadCategories = {},
            onReminderEnabledChange = {},
            onReminderTimeChange = { _, _ -> }
        )
    }
}
