package com.dosparta.trivia.ui.screens

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.foundation.relocation.bringIntoViewRequester
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.components.LoadingScreen
import com.dosparta.trivia.ui.viewmodel.TriviaUiState
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import kotlinx.coroutines.delay

private const val ANSWER_AUTO_ADVANCE_DELAY_MILLIS = 5_000L
private val CorrectAnswerColor = Color(0xFF2E7D32)
private val CorrectAnswerContentColor = Color(0xFFFFFFFF)
private val NextQuestionButtonFillColor = Color(0xFF1D4ED8)

/**
 * The main trivia screen, showing questions, handling user answers,
 * and delegating to [onResult] when the game finishes.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TriviaScreen(
    viewModel: TriviaViewModel = hiltViewModel(),
    onResult: (GameResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()

    DisposableEffect(viewModel) {
        onDispose { viewModel.cancelPendingLoad() }
    }

    LaunchedEffect(Unit) {
        if (categories.isEmpty()) {
            viewModel.loadCategories()
        }
    }

    LaunchedEffect(uiState) {
        if (uiState is TriviaUiState.Result) {
            onResult((uiState as TriviaUiState.Result).result)
        }
    }

    when (val state = uiState) {
        is TriviaUiState.Loading -> LoadingScreen()
        is TriviaUiState.Error -> ErrorScreen(
            message = state.message,
            onRetry = { viewModel.loadQuestions() }
        )
        is TriviaUiState.Result -> {
            // Navigation is triggered in the side effect above; the UI doesn't need to render it.
        }
        is TriviaUiState.Game -> {
            val current = state.session
            val question = current.currentQuestion
            val selectedAnswerState = remember(current.currentIndex, question?.question) {
                mutableStateOf<String?>(null)
            }
            val nextQuestionButtonBringIntoViewRequester = remember(current.currentIndex, question?.question) {
                BringIntoViewRequester()
            }
            val selectedAnswer = selectedAnswerState.value

            if (question == null) {
                ErrorScreen(
                    message = UiText.StringResource(R.string.error_no_valid_question),
                    onRetry = { viewModel.loadQuestions() }
                )
                return
            }

            Scaffold(
                modifier = Modifier.testTag("trivia_screen"),
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
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .padding(16.dp)
                        .verticalScroll(rememberScrollState()),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Text(
                        text = stringResource(R.string.question_counter, current.currentIndex + 1, current.questions.size),
                        style = MaterialTheme.typography.titleMedium
                    )

                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Surface(
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Category: ${question.category}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }

                        Surface(
                            tonalElevation = 1.dp,
                            shape = MaterialTheme.shapes.small,
                            color = MaterialTheme.colorScheme.surfaceVariant
                        ) {
                            Text(
                                text = "Difficulty: ${question.difficulty.replaceFirstChar { it.uppercase() }}",
                                style = MaterialTheme.typography.labelLarge,
                                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                            )
                        }
                    }

                    Card(
                        modifier = Modifier.fillMaxWidth(),
                        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
                    ) {
                        Text(
                            text = question.question,
                            style = MaterialTheme.typography.titleLarge,
                            modifier = Modifier
                                .padding(16.dp)
                                .testTag("question_text")
                        )
                    }

                    AnswerOptionsSection(
                        question = question,
                        selectedAnswer = selectedAnswer,
                        onAnswerSelected = { selectedAnswerState.value = it },
                        onAnswerConfirmed = { viewModel.submit(it) },
                        bringIntoViewRequester = nextQuestionButtonBringIntoViewRequester
                    )
                }
            }
        }
        // todo why is this needed? Could TriviaNavHost handle this?
        TriviaUiState.Idle -> SetupScreen(
            categories = categories,
            categoriesError = null,
            reminderEnabled = false,
            reminderHour = 19,
            reminderMinute = 0,
            onStartGame = { config ->
                viewModel.loadQuestions(config)
            },
            onRetryLoadCategories = { viewModel.loadCategories() },
            onReminderEnabledChange = {},
            onReminderTimeChange = { _, _ -> }
        )
    }
}

@Composable
internal fun AnswerOptionsSection(
    question: TriviaQuestion,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onAnswerConfirmed: (String) -> Unit,
    bringIntoViewRequester: BringIntoViewRequester,
    autoAdvanceDelayMillis: Long = ANSWER_AUTO_ADVANCE_DELAY_MILLIS
) {
    val autoAdvanceProgress = remember(question.question) { Animatable(0f) }

    LaunchedEffect(selectedAnswer) {
        autoAdvanceProgress.snapTo(0f)
        val answer = selectedAnswer ?: return@LaunchedEffect
        bringIntoViewRequester.bringIntoView()
        autoAdvanceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = autoAdvanceDelayMillis.toInt(),
                easing = LinearEasing
            )
        )
        onAnswerConfirmed(answer)
    }

    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        question.options.forEachIndexed { index, option ->
            val answerState = when {
                selectedAnswer == null -> "default"
                option == question.correctAnswer -> "correct"
                option == selectedAnswer -> "incorrect"
                else -> "default"
            }
            val buttonColors = when (answerState) {
                "correct" -> ButtonDefaults.buttonColors(
                    containerColor = CorrectAnswerColor,
                    contentColor = CorrectAnswerContentColor
                )
                "incorrect" -> ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.error,
                    contentColor = MaterialTheme.colorScheme.onError
                )
                else -> ButtonDefaults.buttonColors()
            }
            Button(
                onClick = {
                    if (selectedAnswer == null) {
                        onAnswerSelected(option)
                    }
                },
                colors = buttonColors,
                modifier = Modifier
                    .fillMaxWidth()
                    .testTag("answer_option_$index")
                    .semantics {
                        stateDescription = answerState
                    }
            ) {
                Text(text = option)
            }
        }

        if (selectedAnswer != null) {
            Button(
                onClick = { onAnswerConfirmed(selectedAnswer) },
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.tertiary,
                    contentColor = MaterialTheme.colorScheme.onTertiary
                ),
                contentPadding = PaddingValues(0.dp),
                modifier = Modifier
                    .padding(top = 16.dp, start = 100.dp)
                    .fillMaxWidth()
                    .height(48.dp)
                    .bringIntoViewRequester(bringIntoViewRequester)
                    .testTag("next_question_button")
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(MaterialTheme.shapes.large)
                ) {
                    Box(
                        modifier = Modifier
                            .fillMaxHeight()
                            .fillMaxWidth(autoAdvanceProgress.value)
                            .background(NextQuestionButtonFillColor)
                    )
                    Text(
                        text = stringResource(R.string.next_question),
                        modifier = Modifier.align(Alignment.Center)
                    )
                }
            }
        }
    }
}
