package com.dosparta.trivia.ui.screens

import android.os.Build
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onLast
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import com.dosparta.core.ui.theme.TriviaGame2Theme
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.ui.currentContentLanguage
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertEquals
import kotlin.test.assertNull

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class LocalizationScreenTests {
    @get:Rule
    val composeTestRule = createComposeRule()

    private val booleanQuestion = TriviaQuestion(
        category = "Science",
        type = "boolean",
        difficulty = "hard",
        question = "Is water wet?",
        correctAnswer = "True",
        options = listOf("True", "False")
    )

    @Test
    fun `language picker reports the chosen language and system default`() {
        val selections = mutableListOf<AppLanguage?>()
        setSetupContent(onLanguageSelected = { selections += it })

        composeTestRule.onNodeWithTag("language_button").performScrollTo().performClick()
        composeTestRule.onNodeWithText("Deutsch").performClick()
        composeTestRule.onNodeWithTag("language_button").performScrollTo().performClick()
        // The closed field also displays "System default", so pick the menu item.
        composeTestRule.onAllNodesWithText("System default").onLast().performClick()

        composeTestRule.runOnIdle {
            assertEquals(listOf(AppLanguage.GERMAN, null), selections)
        }
    }

    @Test
    fun `setup shows the selected override`() {
        setSetupContent(selectedLanguage = AppLanguage.CHINESE_SIMPLIFIED)

        composeTestRule.onNodeWithText("简体中文").performScrollTo().assertIsDisplayed()
    }

    @Test
    fun `translation notice is only shown when translation failed`() {
        setSetupContent(translationUnavailable = true)

        composeTestRule.onNodeWithTag("translation_notice").assertIsDisplayed()
    }

    @Test
    fun `translation notice is hidden by default`() {
        setSetupContent()

        composeTestRule.onNodeWithTag("translation_notice").assertDoesNotExist()
    }

    @Test
    @Config(qualifiers = "de")
    fun `german ui localizes setup labels and resolves german content language`() {
        var language: AppLanguage? = null
        composeTestRule.setContent {
            language = currentContentLanguage()
            TriviaGame2Theme {
                SetupScreen(
                    categories = listOf(TriviaCategory(9, "Allgemeinwissen")),
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

        composeTestRule.onNodeWithText("Spiel starten").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Systemstandard").performScrollTo().assertIsDisplayed()
        composeTestRule.runOnIdle { assertEquals(AppLanguage.GERMAN, language) }
    }

    @Test
    @Config(qualifiers = "es")
    fun `boolean answers and difficulty are localized but submit the original value`() {
        val confirmed = mutableListOf<String>()
        composeTestRule.setContent {
            TriviaGame2Theme {
                TriviaGameContent(
                    session = GameSession(questions = listOf(booleanQuestion), startTimeMillis = 0L),
                    onAnswerConfirmed = { confirmed += it },
                    onRetry = {}
                )
            }
        }

        composeTestRule.onNodeWithText("Dificultad: Difícil").assertIsDisplayed()
        composeTestRule.onNodeWithText("Verdadero").performScrollTo().assertIsDisplayed()
        composeTestRule.onNodeWithText("Falso").performScrollTo().performClick()

        // Idling lets the auto-advance countdown finish and confirm the answer.
        composeTestRule.runOnIdle { assertEquals("False", confirmed.first()) }
    }

    @Test
    @Config(qualifiers = "zh-rTW")
    fun `traditional chinese locale falls back to english content`() {
        var language: AppLanguage? = AppLanguage.CHINESE_SIMPLIFIED
        composeTestRule.setContent { language = currentContentLanguage() }

        composeTestRule.runOnIdle { assertEquals(AppLanguage.ENGLISH, language) }
    }

    @Test
    fun `default ui resolves english content language`() {
        var language: AppLanguage? = null
        composeTestRule.setContent { language = currentContentLanguage() }

        composeTestRule.runOnIdle {
            assertEquals(AppLanguage.ENGLISH, language)
            assertNull(AppLanguage.fromTag("fr"))
        }
    }

    private fun setSetupContent(
        selectedLanguage: AppLanguage? = null,
        onLanguageSelected: (AppLanguage?) -> Unit = {},
        translationUnavailable: Boolean = false
    ) {
        composeTestRule.setContent {
            TriviaGame2Theme {
                SetupScreen(
                    categories = listOf(TriviaCategory(9, "General Knowledge")),
                    categoriesError = null,
                    reminderEnabled = false,
                    reminderHour = 19,
                    reminderMinute = 0,
                    onStartGame = {},
                    onRetryLoadCategories = {},
                    onReminderEnabledChange = {},
                    onReminderTimeChange = { _, _ -> },
                    selectedLanguage = selectedLanguage,
                    onLanguageSelected = onLanguageSelected,
                    translationUnavailable = translationUnavailable
                )
            }
        }
    }
}
