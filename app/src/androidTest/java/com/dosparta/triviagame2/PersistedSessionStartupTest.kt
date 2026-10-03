package com.dosparta.triviagame2

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.ui.screens.TriviaScreen
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class PersistedSessionStartupTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun persisted_session_navigates_to_trivia_screen() {
        val persistedQuestion = TriviaQuestion(
            category = "Science",
            type = "multiple",
            difficulty = "easy",
            question = "What is H2O?",
            correctAnswer = "Water",
            options = listOf("Water", "Salt", "Sugar", "Ice")
        )
        val persistedSession = GameSessionState(
            questions = listOf(persistedQuestion),
            currentIndex = 0,
            correctCount = 0,
            activeElapsedMillis = 42L,
            selectedAnswers = emptyMap()
        )

        val fakeSessionRepo = object : IGameSessionRepository {
            override suspend fun saveGameSession(
                questions: List<TriviaQuestion>,
                currentIndex: Int,
                correctCount: Int,
                activeElapsedMillis: Long,
                selectedAnswers: Map<Int, String>
            ) = Unit

            override fun getActiveSessionFlow(): Flow<GameSessionState?> = flowOf(persistedSession)

            override suspend fun getActiveSession(): GameSessionState? = persistedSession

            override suspend fun clearActiveSession() = Unit
        }

        val fakeRepo = object : ITriviaRepository {
            override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> = listOf(persistedQuestion)

            override suspend fun getCategories(): List<TriviaCategory> = emptyList()
        }

        val localizationRepository = object : IContentLocalizationRepository {
            override val contentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
            override val translationUnavailable = MutableStateFlow(false)
            override fun setContentLanguage(language: AppLanguage) {
                contentLanguage.value = language
            }
            override fun setTranslationUnavailable(unavailable: Boolean) {
                translationUnavailable.value = unavailable
            }
        }
        val viewModel = TriviaViewModel(
            sdk = TriviaSdk(fakeRepo, fakeSessionRepo, localizationRepository)
        )

        composeRule.setContent {
            TriviaScreen(
                viewModel = viewModel,
                onResult = {}
            )
        }

        composeRule.runOnIdle {
            viewModel.bootstrapApp()
        }

        composeRule.waitUntil(10_000L) {
            composeRule.onAllNodesWithTag("question_text").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("trivia_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("question_text").assertIsDisplayed()
        composeRule.onNodeWithText("What is H2O?").assertIsDisplayed()
    }
}
