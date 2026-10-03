package com.dosparta.trivia.ui.screens

import android.content.Context
import android.os.Build
import androidx.compose.foundation.ScrollState
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.assertTextEquals
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.core.ui.theme.TriviaGame2Theme
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
    fun `finish game button finishes only after confirmation`() {
        var finishCalls = 0
        composeTestRule.setContent {
            TriviaGame2Theme {
                TriviaGameContent(
                    session = GameSession(questions = listOf(gameQuestion(), gameQuestion()), startTimeMillis = 0L),
                    onAnswerConfirmed = {},
                    onRetry = {},
                    onFinishGame = { finishCalls++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("finish_game_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("finish_game_dialog").assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(0, finishCalls) }

        composeTestRule.onNodeWithTag("finish_game_confirm_button").performClick()

        composeTestRule.runOnIdle { assertEquals(1, finishCalls) }
        composeTestRule.onNodeWithTag("finish_game_dialog").assertDoesNotExist()
    }

    @Test
    fun `cancelling finish game dialog keeps the game going`() {
        var finishCalls = 0
        composeTestRule.setContent {
            TriviaGame2Theme {
                TriviaGameContent(
                    session = GameSession(questions = listOf(gameQuestion()), startTimeMillis = 0L),
                    onAnswerConfirmed = {},
                    onRetry = {},
                    onFinishGame = { finishCalls++ }
                )
            }
        }

        composeTestRule.onNodeWithTag("finish_game_button").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("finish_game_cancel_button").performClick()

        composeTestRule.onNodeWithTag("finish_game_dialog").assertDoesNotExist()
        composeTestRule.onNodeWithTag("question_text").assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(0, finishCalls) }
    }

    @Test
    fun `game content auto confirms and resets for next index with identical question text`() {
        val question = gameQuestion()
        val session = mutableStateOf(
            GameSession(questions = listOf(question, question), startTimeMillis = 0L)
        )
        val confirmedAnswers = mutableListOf<String>()
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeTestRule.setContent {
            TriviaGame2Theme {
                TriviaGameContent(
                    session = session.value,
                    onAnswerConfirmed = {
                        confirmedAnswers += it
                        session.value = session.value.copy(currentIndex = 1)
                    },
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithTag("question_text").assertTextEquals(question.question)
        composeTestRule.onNodeWithText(context.getString(R.string.question_counter, 1, 2)).assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_option_1").performScrollTo().performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf("Salt"), confirmedAnswers)
        }
        composeTestRule.onNodeWithText(context.getString(R.string.question_counter, 2, 2))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithTag("answer_option_1").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "default")
        )
        composeTestRule.onNodeWithTag("next_question_button").assertDoesNotExist()
    }

    @Test
    fun `game content resets selection when question text changes at the same index`() {
        val question = gameQuestion()
        val session = mutableStateOf(GameSession(questions = listOf(question), startTimeMillis = 0L))
        composeTestRule.setContent {
            TriviaGame2Theme {
                TriviaGameContent(session = session.value, onAnswerConfirmed = {}, onRetry = {})
            }
        }
        composeTestRule.onNodeWithTag("answer_option_0").performScrollTo().performClick()
        composeTestRule.onNodeWithTag("answer_option_0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "correct")
        )
        composeTestRule.runOnIdle {
            session.value = session.value.copy(questions = listOf(question.copy(question = "What do we drink?")))
        }
        composeTestRule.onNodeWithTag("question_text").assertTextEquals("What do we drink?")
        composeTestRule.onNodeWithTag("answer_option_0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "default")
        )
        composeTestRule.onNodeWithTag("next_question_button").assertDoesNotExist()
    }

    @Test
    fun `game content without a valid question offers retry`() {
        var retried = false
        val context = ApplicationProvider.getApplicationContext<Context>()
        composeTestRule.setContent {
            TriviaGame2Theme {
                TriviaGameContent(
                    session = GameSession(questions = emptyList(), startTimeMillis = 0L),
                    onAnswerConfirmed = {},
                    onRetry = { retried = true }
                )
            }
        }
        composeTestRule.onNodeWithText(context.getString(R.string.error_no_valid_question)).assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.retry)).performClick()
        assertTrue(retried)
    }

    @Test
    fun `shared theme preserves brand light and dark palettes and typography`() {
        val dark = mutableStateOf(false)
        var primary = Color.Unspecified
        var fontSize = androidx.compose.ui.unit.TextUnit.Unspecified
        composeTestRule.setContent {
            TriviaGame2Theme(darkTheme = dark.value) {
                primary = MaterialTheme.colorScheme.primary
                fontSize = MaterialTheme.typography.bodyLarge.fontSize
            }
        }
        composeTestRule.runOnIdle {
            assertEquals(Color(0xFF4F46E5), primary)
            assertEquals(16f, fontSize.value)
            dark.value = true
        }
        composeTestRule.runOnIdle {
            assertEquals(Color(0xFFC3C4FF), primary)
            assertEquals(16f, fontSize.value)
        }
    }

    private fun gameQuestion() = TriviaQuestion(
        category = "Science",
        type = "multiple",
        difficulty = "easy",
        question = "What is H2O?",
        correctAnswer = "Water",
        options = listOf("Water", "Salt", "Sugar", "Ice")
    )

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

        composeTestRule.onNodeWithText(context.getString(R.string.score_label, 3, 4))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.incorrect_label, 1))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.percentage_label, "75.0"))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.time_label, "2:05"))
            .performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText(context.getString(R.string.play_again))
            .performScrollTo().assertIsDisplayed().performClick()
        composeTestRule.onNodeWithText(context.getString(R.string.start_new_game))
            .performScrollTo().assertIsDisplayed().performClick()
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
                    scrollState = rememberScrollState()
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

    @Test
    fun `selecting an answer scrolls the confirm button into view`() {
        val question = gameQuestion()
        val selectedAnswer = mutableStateOf<String?>(null)
        lateinit var scrollState: ScrollState

        composeTestRule.setContent {
            TriviaGame2Theme {
                scrollState = rememberScrollState()
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                ) {
                    // Tall enough that the confirm button starts well below the viewport.
                    Spacer(modifier = Modifier.height(1_200.dp))
                    AnswerOptionsSection(
                        question = question,
                        selectedAnswer = selectedAnswer.value,
                        onAnswerSelected = { selectedAnswer.value = it },
                        onAnswerConfirmed = {},
                        scrollState = scrollState
                    )
                }
            }
        }

        composeTestRule.onNodeWithText("Salt").performScrollTo().performClick()

        composeTestRule.waitUntil(5_000L) {
            scrollState.maxValue > 0 && scrollState.value == scrollState.maxValue
        }
        composeTestRule.onNodeWithTag("next_question_button").assertIsDisplayed()
    }

}
