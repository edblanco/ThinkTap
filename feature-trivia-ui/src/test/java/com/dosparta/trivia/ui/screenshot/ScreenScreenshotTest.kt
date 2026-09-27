package com.dosparta.trivia.ui.screenshot

import androidx.compose.foundation.rememberScrollState
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.components.LoadingScreen
import com.dosparta.trivia.ui.preview.PreviewFixtures
import com.dosparta.trivia.ui.screens.AnswerOptionsSection
import com.dosparta.trivia.ui.screens.ResultScreen
import com.dosparta.trivia.ui.screens.SetupScreen
import com.dosparta.trivia.ui.screens.StartupScreen
import com.dosparta.trivia.ui.screens.TriviaGameContent
import com.dosparta.core.ui.screenshot.ScreenshotTest
import org.junit.Test

/**
 * Golden images for every screen state that carries distinct visual meaning.
 *
 * These complement [com.dosparta.trivia.ui.screens.ScreenTests], which asserts behaviour and
 * semantics. Nothing there would notice a broken palette, a collapsed type scale, or a layout
 * that started wrapping, which is exactly what these images pin down.
 */
class ScreenScreenshotTest : ScreenshotTest() {

    @Test
    fun setupScreenLight() = captureSetup("setup_screen_light", darkTheme = false)

    @Test
    fun setupScreenDark() = captureSetup("setup_screen_dark", darkTheme = true)

    @Test
    fun setupScreenReminderEnabled() = captureScreen("setup_screen_reminder_enabled") {
        SetupScreen(
            categories = PreviewFixtures.categories,
            categoriesError = null,
            reminderEnabled = true,
            reminderHour = REMINDER_HOUR,
            reminderMinute = REMINDER_MINUTE,
            onStartGame = {},
            onRetryLoadCategories = {},
            onReminderEnabledChange = {},
            onReminderTimeChange = { _, _ -> }
        )
    }

    @Test
    fun setupScreenError() = captureScreen("setup_screen_error") {
        SetupScreen(
            categories = emptyList(),
            categoriesError = PreviewFixtures.error,
            reminderEnabled = false,
            reminderHour = REMINDER_HOUR,
            reminderMinute = REMINDER_MINUTE,
            onStartGame = {},
            onRetryLoadCategories = {},
            onReminderEnabledChange = {},
            onReminderTimeChange = { _, _ -> }
        )
    }

    @Test
    fun triviaGameLight() = captureGame("trivia_game_light", darkTheme = false)

    @Test
    fun triviaGameDark() = captureGame("trivia_game_dark", darkTheme = true)

    @Test
    fun answerOptionsUnanswered() = captureScreen("answer_options_unanswered") {
        AnswerOptionsSection(
            question = PreviewFixtures.question,
            selectedAnswer = null,
            onAnswerSelected = {},
            onAnswerConfirmed = {},
            scrollState = rememberScrollState()
        )
    }

    @Test
    fun answerOptionsCorrect() = captureAnswered(
        name = "answer_options_correct",
        selectedAnswer = PreviewFixtures.question.correctAnswer
    )

    @Test
    fun answerOptionsIncorrect() = captureAnswered(
        name = "answer_options_incorrect",
        selectedAnswer = PreviewFixtures.question.options.first()
    )

    @Test
    fun resultScreenPartial() = captureResult("result_screen_partial", PreviewFixtures.result)

    @Test
    fun resultScreenPerfect() = captureResult(
        name = "result_screen_perfect",
        result = GameResult(
            totalQuestions = TOTAL_QUESTIONS,
            correctAnswers = TOTAL_QUESTIONS,
            durationMillis = DURATION_MILLIS
        )
    )

    @Test
    fun resultScreenZero() = captureResult(
        name = "result_screen_zero",
        result = GameResult(
            totalQuestions = TOTAL_QUESTIONS,
            correctAnswers = 0,
            durationMillis = DURATION_MILLIS
        )
    )

    @Test
    fun startupScreenSkeleton() = captureScreen("startup_screen_skeleton") {
        StartupScreen(error = null, onRetry = {})
    }

    @Test
    fun startupScreenError() = captureScreen("startup_screen_error") {
        StartupScreen(error = PreviewFixtures.error, onRetry = {})
    }

    @Test
    fun loadingScreen() = captureScreen("loading_screen") { LoadingScreen() }

    @Test
    fun errorScreen() = captureScreen("error_screen") {
        ErrorScreen(message = PreviewFixtures.error, onRetry = {})
    }

    private fun captureSetup(name: String, darkTheme: Boolean) =
        captureScreen(name, darkTheme = darkTheme) {
            SetupScreen(
                categories = PreviewFixtures.categories,
                categoriesError = null,
                reminderEnabled = false,
                reminderHour = REMINDER_HOUR,
                reminderMinute = REMINDER_MINUTE,
                onStartGame = {},
                onRetryLoadCategories = {},
                onReminderEnabledChange = {},
                onReminderTimeChange = { _, _ -> }
            )
        }

    private fun captureGame(name: String, darkTheme: Boolean) =
        captureScreen(name, darkTheme = darkTheme) {
            TriviaGameContent(
                session = PreviewFixtures.session,
                onAnswerConfirmed = {},
                onRetry = {}
            )
        }

    private fun captureResult(name: String, result: GameResult) = captureScreen(name) {
        ResultScreen(result = result, onPlayAgain = {}, onStartNewGame = {})
    }

    /**
     * Captures a graded answer list shortly after selection, once the confirm button has finished
     * expanding but while the auto-advance countdown is still near its start.
     */
    private fun captureAnswered(name: String, selectedAnswer: String) = captureScreen(
        name = name,
        advanceTimeMillis = SETTLE_MILLIS
    ) {
        AnswerOptionsSection(
            question = PreviewFixtures.question,
            selectedAnswer = selectedAnswer,
            onAnswerSelected = {},
            onAnswerConfirmed = {},
            scrollState = rememberScrollState()
        )
    }

    private companion object {
        const val REMINDER_HOUR = 9
        const val REMINDER_MINUTE = 30
        const val TOTAL_QUESTIONS = 10
        const val DURATION_MILLIS = 125_000L

        /** Long enough for the reveal transition to finish, short enough to keep the countdown early. */
        const val SETTLE_MILLIS = 500L
    }
}
