package com.dosparta.trivia.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.relocation.BringIntoViewRequester
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class ScreenTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    @Test
    fun `ErrorScreen shows message and retry button`() {
        var retryClicked = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        val expectedMessage = context.getString(R.string.error_network_occurred)
        val expectedRetry = context.getString(R.string.retry)

        composeTestRule.setContent {
            ErrorScreen(
                message = UiText.StringResource(R.string.error_network_occurred),
                onRetry = { retryClicked = true }
            )
        }

        composeTestRule.onNodeWithText(expectedMessage).assertIsDisplayed()
        composeTestRule.onNodeWithText(expectedRetry).assertIsDisplayed().performClick()
        assertTrue(retryClicked)
    }

    @Test
    fun `ResultScreen displays correct stats and handles actions`() {
        var playAgainClicked = false
        var startNewGameClicked = false
        val result = GameResult(
            totalQuestions = 4,
            correctAnswers = 3,
            durationMillis = 125_000L  // 2:05
        )
        val context = ApplicationProvider.getApplicationContext<Context>()

        composeTestRule.setContent {
            ResultScreen(
                result = result,
                onPlayAgain = { playAgainClicked = true },
                onStartNewGame = { startNewGameClicked = true }
            )
        }

        composeTestRule.onNodeWithText(context.getString(R.string.score_label, 3, 4)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.incorrect_label, 1)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.percentage_label, "75.0")).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.time_label, "2:05")).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.play_again)).assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.start_new_game)).assertIsDisplayed().performClick()
        assertTrue(playAgainClicked)
        assertTrue(startNewGameClicked)
    }

    @Test
    fun `AnswerOptionsSection highlights incorrect and correct answers`() {
        val question = TriviaQuestion(
            category = "Science",
            type = "multiple",
            difficulty = "easy",
            question = "What is H2O?",
            correctAnswer = "Water",
            options = listOf("Water", "Salt", "Sugar", "Ice")
        )
        val selectedAnswer = mutableStateOf<String?>(null)
        var confirmedAnswer: String? = null

        composeTestRule.setContent {
            MaterialTheme {
                AnswerOptionsSection(
                    question = question,
                    selectedAnswer = selectedAnswer.value,
                    onAnswerSelected = { selectedAnswer.value = it },
                    onAnswerConfirmed = { confirmedAnswer = it },
                    bringIntoViewRequester = remember { BringIntoViewRequester() }
                )
            }
        }

        composeTestRule.onNodeWithText("Salt").performClick()
        composeTestRule.waitUntil(1_000L) {
            composeTestRule.onAllNodesWithTag("next_question_button").fetchSemanticsNodes().isNotEmpty()
        }

        composeTestRule.onNodeWithTag("answer_option_1").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "incorrect")
        )
        composeTestRule.onNodeWithTag("answer_option_0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "correct")
        )
        composeTestRule.onNodeWithTag("next_question_button").assertIsDisplayed().performClick()
        assertEquals("Salt", confirmedAnswer)
    }

}
