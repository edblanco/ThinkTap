package com.dosparta.triviagame2

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createComposeRule
import androidx.compose.ui.test.onAllNodesWithTag
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performSemanticsAction
import androidx.compose.ui.semantics.SemanticsActions
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import com.dosparta.trivia.domain.usecase.ClearGameSessionUseCase
import com.dosparta.trivia.domain.usecase.FinishGameUseCase
import com.dosparta.trivia.domain.usecase.LoadCategoriesUseCase
import com.dosparta.trivia.domain.usecase.PersistGameSessionUseCase
import com.dosparta.trivia.domain.usecase.ResolveAppStartupUseCase
import com.dosparta.trivia.domain.usecase.StartGameSession
import com.dosparta.trivia.domain.usecase.SubmitAnswerUseCase
import com.dosparta.trivia.ui.screens.SetupScreen
import com.dosparta.trivia.ui.screens.TriviaScreen
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import java.util.concurrent.atomic.AtomicReference

@RunWith(AndroidJUnit4::class)
class SetupToTriviaTest {

    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun setup_screen_starts_game_and_navigates_to_trivia() {
        val selectedConfig = AtomicReference<TriviaConfig?>()
        val categories = listOf(TriviaCategory(17, "Science"))
        val firstQuestion = TriviaQuestion(
            category = "Science",
            type = "multiple",
            difficulty = "easy",
            question = "What is H2O?",
            correctAnswer = "Water",
            options = listOf("Water", "Salt", "Sugar", "Ice")
        )
        val fakeSessionRepo = object : IGameSessionRepository {
            override suspend fun saveGameSession(
                questions: List<TriviaQuestion>,
                currentIndex: Int,
                correctCount: Int,
                activeElapsedMillis: Long,
                selectedAnswers: Map<Int, String>
            ) = Unit

            override fun getActiveSessionFlow(): Flow<GameSessionState?> = flowOf(null)

            override suspend fun getActiveSession(): GameSessionState? = null

            override suspend fun clearActiveSession() = Unit
        }
        val fakeRepo = object : ITriviaRepository {
            override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> {
                selectedConfig.set(config)
                return listOf(firstQuestion)
            }

            override suspend fun getCategories(): List<TriviaCategory> = categories
        }
        val viewModel = TriviaViewModel(
            startGame = StartGameSession(fakeRepo),
            submitAnswer = SubmitAnswerUseCase(GameEngine()),
            finishGame = FinishGameUseCase(GameEngine()),
            loadCategoriesUseCase = LoadCategoriesUseCase(fakeRepo),
            resolveStartupUseCase = ResolveAppStartupUseCase(fakeSessionRepo),
            persistGameSessionUseCase = PersistGameSessionUseCase(fakeSessionRepo),
            clearGameSessionUseCase = ClearGameSessionUseCase(fakeSessionRepo)
        )
        val showTrivia = mutableStateOf(false)

        composeRule.setContent {
            if (showTrivia.value) {
                TriviaScreen(
                    viewModel = viewModel,
                    onResult = {}
                )
            } else {
                SetupScreen(
                    categories = categories,
                    categoriesError = null,
                    reminderEnabled = false,
                    reminderHour = 19,
                    reminderMinute = 0,
                    onStartGame = { config ->
                        viewModel.loadQuestions(config)
                        showTrivia.value = true
                    },
                    onRetryLoadCategories = {},
                    onReminderEnabledChange = {},
                    onReminderTimeChange = { _, _ -> }
                )
            }
        }

        composeRule.onNodeWithTag("setup_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("amount_slider").performSemanticsAction(SemanticsActions.SetProgress) {
            it(20f)
        }
        composeRule.onNodeWithTag("category_button").performClick()
        composeRule.onNodeWithText("Science").performClick()
        composeRule.onNodeWithTag("difficulty_button").performClick()
        composeRule.onNodeWithText("Easy").performClick()
        composeRule.onNodeWithTag("start_game_button").performClick()

        composeRule.waitUntil(10_000L) {
            composeRule.onAllNodesWithTag("question_text").fetchSemanticsNodes().isNotEmpty()
        }

        composeRule.onNodeWithTag("trivia_screen").assertIsDisplayed()
        composeRule.onNodeWithTag("question_text").assertIsDisplayed()
        composeRule.onNodeWithText("What is H2O?").assertIsDisplayed()
        composeRule.runOnIdle {
            val config = selectedConfig.get()
            check(config != null)
            check(config.amount == 20)
            check(config.categoryId == 17)
            check(config.difficulty == "easy")
        }
    }
}
