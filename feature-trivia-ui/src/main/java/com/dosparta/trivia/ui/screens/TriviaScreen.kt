package com.dosparta.trivia.ui.screens

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.expandVertically
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.stateDescription
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.tooling.preview.PreviewLightDark
import androidx.compose.ui.unit.dp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.dosparta.core.ui.components.AnswerOptionButton
import com.dosparta.core.ui.components.AnswerVisualState
import com.dosparta.core.ui.components.CountdownFill
import com.dosparta.core.ui.components.EnterAnimated
import com.dosparta.core.ui.components.TriviaCard
import com.dosparta.core.ui.components.TriviaMetaChip
import com.dosparta.core.ui.components.TriviaProgressBar
import com.dosparta.core.ui.components.TriviaScreenScaffold
import com.dosparta.core.ui.components.TriviaTextButton
import com.dosparta.core.ui.theme.LocalReducedMotion
import com.dosparta.core.ui.theme.quizColors
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.components.TranslationNotice
import com.dosparta.trivia.ui.localizedAnswer
import com.dosparta.trivia.ui.localizedDifficulty
import com.dosparta.trivia.ui.components.LoadingScreen
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.preview.TriviaPreviewTheme
import com.dosparta.trivia.ui.viewmodel.TriviaUiState
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import kotlinx.coroutines.flow.collectLatest

private const val ANSWER_AUTO_ADVANCE_DELAY_MILLIS = 5_000L
private const val STATE_DEFAULT = "default"
private const val STATE_CORRECT = "correct"
private const val STATE_INCORRECT = "incorrect"
private val NextButtonHeight = 56.dp

/**
 * The main trivia screen, showing questions, handling user answers,
 * and delegating to [onResult] when the game finishes.
 */
@Composable
fun TriviaScreen(
    viewModel: TriviaViewModel = hiltViewModel(),
    onResult: (GameResult) -> Unit
) {
    val uiState by viewModel.uiState.collectAsState()
    val categories by viewModel.categories.collectAsState()
    val translationUnavailable by viewModel.translationUnavailable.collectAsState()

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
        is TriviaUiState.Game -> TriviaGameContent(
            session = state.session,
            onAnswerConfirmed = { viewModel.submit(it) },
            onRetry = { viewModel.loadQuestions() },
            onFinishGame = { viewModel.finishEarly() },
            translationUnavailable = translationUnavailable
        )
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
            onReminderTimeChange = { _, _ -> },
            translationUnavailable = translationUnavailable
        )
    }
}

/**
 * The stateless body of the trivia screen.
 *
 * @param session the in-progress game
 * @param onAnswerConfirmed called with the chosen answer once it is committed
 * @param onRetry called when the session has no usable question and the user asks to retry
 * @param onFinishGame called once the user confirms ending the game before answering every question
 * @param translationUnavailable shows a notice that the content fell back to English
 */
@Composable
internal fun TriviaGameContent(
    session: GameSession,
    onAnswerConfirmed: (String) -> Unit,
    onRetry: () -> Unit,
    onFinishGame: () -> Unit = {},
    translationUnavailable: Boolean = false
) {
    val question = session.currentQuestion
    val selectedAnswerState = remember(session.currentIndex, question?.question) {
        mutableStateOf<String?>(null)
    }
    val scrollState = rememberScrollState()
    var showFinishConfirmation by rememberSaveable { mutableStateOf(false) }

    if (question == null) {
        ErrorScreen(message = UiText.StringResource(R.string.error_no_valid_question), onRetry = onRetry)
        return
    }

    TriviaScreenScaffold(title = stringResource(R.string.app_title)) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .testTag("trivia_screen")
                .verticalScroll(scrollState)
                .padding(horizontal = 20.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            if (translationUnavailable) {
                TranslationNotice()
            }

            QuestionHeader(session = session, question = question)

            EnterAnimated(
                modifier = Modifier.fillMaxWidth(),
                key = question.question
            ) {
                TriviaCard(modifier = Modifier.fillMaxWidth()) {
                    Text(
                        text = question.question,
                        style = MaterialTheme.typography.headlineSmall,
                        modifier = Modifier
                            .padding(20.dp)
                            .testTag("question_text")
                    )
                }
            }

            AnswerOptionsSection(
                question = question,
                selectedAnswer = selectedAnswerState.value,
                onAnswerSelected = { selectedAnswerState.value = it },
                onAnswerConfirmed = onAnswerConfirmed,
                scrollState = scrollState
            )

            TriviaTextButton(
                text = stringResource(R.string.finish_game),
                onClick = { showFinishConfirmation = true },
                modifier = Modifier
                    .align(Alignment.CenterHorizontally)
                    .testTag("finish_game_button")
            )
        }
    }

    if (showFinishConfirmation) {
        FinishGameConfirmationDialog(
            onConfirm = {
                showFinishConfirmation = false
                onFinishGame()
            },
            onDismiss = { showFinishConfirmation = false }
        )
    }
}

@Composable
private fun FinishGameConfirmationDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.finish_game_confirm_title)) },
        text = { Text(stringResource(R.string.finish_game_confirm_message)) },
        confirmButton = {
            TextButton(
                onClick = onConfirm,
                modifier = Modifier.testTag("finish_game_confirm_button")
            ) {
                Text(stringResource(R.string.finish_game_confirm))
            }
        },
        dismissButton = {
            TextButton(
                onClick = onDismiss,
                modifier = Modifier.testTag("finish_game_cancel_button")
            ) {
                Text(stringResource(R.string.cancel))
            }
        },
        modifier = Modifier.testTag("finish_game_dialog")
    )
}

@Composable
private fun QuestionHeader(session: GameSession, question: TriviaQuestion) {
    val questionNumber = session.currentIndex + 1
    val total = session.questions.size

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            text = stringResource(R.string.question_counter, questionNumber, total),
            style = MaterialTheme.typography.titleMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )

        TriviaProgressBar(
            progress = if (total == 0) 0f else questionNumber.toFloat() / total,
            contentDescription = stringResource(R.string.quiz_progress_description, questionNumber, total)
        )

        FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
            maxLines = 2) {
            TriviaMetaChip(text = stringResource(R.string.category_value, question.category))
            TriviaMetaChip(
                text = stringResource(
                    R.string.difficulty_value,
                    localizedDifficulty(question.difficulty)
                ),
                containerColor = MaterialTheme.colorScheme.tertiaryContainer,
                contentColor = MaterialTheme.colorScheme.onTertiaryContainer
            )
        }
    }
}

/**
 * The list of answer options plus the confirm action.
 *
 * Once an answer is chosen the correct and incorrect options reveal themselves and a countdown
 * starts; when it completes the answer is confirmed automatically.
 *
 * @param scrollState the enclosing scroll state, scrolled to the bottom to reveal the confirm
 *   button once an answer is chosen
 * @param autoAdvanceDelayMillis how long the countdown runs before confirming
 */
@Composable
internal fun AnswerOptionsSection(
    question: TriviaQuestion,
    selectedAnswer: String?,
    onAnswerSelected: (String) -> Unit,
    onAnswerConfirmed: (String) -> Unit,
    scrollState: ScrollState,
    autoAdvanceDelayMillis: Long = ANSWER_AUTO_ADVANCE_DELAY_MILLIS
) {
    val autoAdvanceProgress = remember(question.question) { Animatable(0f) }
    val reducedMotion = LocalReducedMotion.current

    ScrollToConfirmButtonEffect(
        active = selectedAnswer != null,
        scrollState = scrollState,
        reducedMotion = reducedMotion
    )

    LaunchedEffect(selectedAnswer) {
        autoAdvanceProgress.snapTo(0f)
        val answer = selectedAnswer ?: return@LaunchedEffect
        autoAdvanceProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(
                durationMillis = autoAdvanceDelayMillis.toInt(),
                easing = LinearEasing
            )
        )
        onAnswerConfirmed(answer)
    }

    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
        question.options.forEachIndexed { index, option ->
            val answerState = answerStateOf(option, selectedAnswer, question.correctAnswer)
            AnswerOptionButton(
                text = localizedAnswer(question, option),
                state = visualStateOf(answerState, selectedAnswer, option),
                onClick = {
                    if (selectedAnswer == null) {
                        onAnswerSelected(option)
                    }
                },
                modifier = Modifier
                    .testTag("answer_option_$index")
                    .semantics { stateDescription = answerState }
            )
        }

        AnimatedVisibility(
            visible = selectedAnswer != null,
            enter = fadeIn() + expandVertically(),
            exit = fadeOut() + shrinkVertically()
        ) {
            NextQuestionButton(
                progress = autoAdvanceProgress.value,
                onClick = { selectedAnswer?.let(onAnswerConfirmed) },
                modifier = Modifier
                    .padding(top = 8.dp)
                    .fillMaxWidth()
                    .height(NextButtonHeight)
                    .testTag("next_question_button")
            )
        }
    }
}

/**
 * Scrolls the enclosing container to the bottom while [active], keeping the confirm button in
 * view as it appears.
 *
 * The button is revealed by an expand animation, so the bottom of the content keeps moving for as
 * long as that animation runs. Rather than scrolling once — which would aim at a position that is
 * immediately stale — this follows `maxValue` and restarts the scroll each time it grows, so the
 * view always settles on the final bottom.
 */
@Composable
private fun ScrollToConfirmButtonEffect(
    active: Boolean,
    scrollState: ScrollState,
    reducedMotion: Boolean
) {
    LaunchedEffect(active, scrollState, reducedMotion) {
        if (!active) return@LaunchedEffect
        snapshotFlow { scrollState.maxValue }
            .collectLatest { maxValue ->
                // maxValue is Int.MAX_VALUE until the content has been measured.
                if (maxValue == 0 || maxValue == Int.MAX_VALUE) return@collectLatest
                if (reducedMotion) {
                    scrollState.scrollTo(maxValue)
                } else {
                    scrollState.animateScrollTo(maxValue)
                }
            }
    }
}

/** Maps an option to the semantics state the tests and screen readers rely on. */
private fun answerStateOf(option: String, selectedAnswer: String?, correctAnswer: String): String = when {
    selectedAnswer == null -> STATE_DEFAULT
    option == correctAnswer -> STATE_CORRECT
    option == selectedAnswer -> STATE_INCORRECT
    else -> STATE_DEFAULT
}

private fun visualStateOf(
    answerState: String,
    selectedAnswer: String?,
    option: String
): AnswerVisualState = when {
    answerState == STATE_CORRECT -> AnswerVisualState.Correct
    answerState == STATE_INCORRECT -> AnswerVisualState.Incorrect
    selectedAnswer != null && option != selectedAnswer -> AnswerVisualState.Dimmed
    else -> AnswerVisualState.Unanswered
}

/**
 * The confirm action, which doubles as the countdown to auto-advance: the fill sweeps across the
 * button so the remaining time is visible without a separate timer.
 */
@Composable
private fun NextQuestionButton(
    progress: Float,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val shape = MaterialTheme.shapes.large
    Button(
        onClick = onClick,
        shape = shape,
        contentPadding = PaddingValues(0.dp),
        colors = ButtonDefaults.buttonColors(
            containerColor = MaterialTheme.colorScheme.secondaryContainer,
            contentColor = MaterialTheme.colorScheme.onSecondaryContainer
        ),
        modifier = modifier
    ) {
        Box(modifier = Modifier.fillMaxSize().clip(shape)) {
            CountdownFill(
                progress = progress,
                color = MaterialTheme.quizColors.timerIndicator.copy(alpha = 0.35f)
            )
            Text(
                text = stringResource(R.string.next_question),
                style = MaterialTheme.typography.labelLarge,
                modifier = Modifier.align(Alignment.Center)
            )
        }
    }
}

@PreviewLightDark
@Composable
private fun TriviaGamePreview() {
    TriviaPreviewTheme {
        TriviaGameContent(session = PreviewFixtures.session, onAnswerConfirmed = {}, onRetry = {})
    }
}

@Preview(widthDp = 360)
@Composable
private fun UnansweredOptionsPreview() {
    TriviaPreviewTheme {
        AnswerOptionsSection(
            question = PreviewFixtures.question,
            selectedAnswer = null,
            onAnswerSelected = {},
            onAnswerConfirmed = {},
            scrollState = rememberScrollState()
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun CorrectAnswerOptionsPreview() {
    TriviaPreviewTheme {
        AnswerOptionsSection(
            question = PreviewFixtures.question,
            selectedAnswer = PreviewFixtures.question.correctAnswer,
            onAnswerSelected = {},
            onAnswerConfirmed = {},
            scrollState = rememberScrollState()
        )
    }
}

@Preview(widthDp = 360)
@Composable
private fun IncorrectAnswerOptionsPreview() {
    TriviaPreviewTheme {
        AnswerOptionsSection(
            question = PreviewFixtures.question,
            selectedAnswer = PreviewFixtures.question.options.first(),
            onAnswerSelected = {},
            onAnswerConfirmed = {},
            scrollState = rememberScrollState()
        )
    }
}
