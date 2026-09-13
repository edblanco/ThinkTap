package com.dosparta.trivia.ui.screens

import android.content.Context
import android.os.Build
import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import androidx.compose.ui.semantics.SemanticsProperties
import androidx.compose.ui.test.assert
import androidx.compose.ui.test.SemanticsMatcher
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithTag
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.core.app.ApplicationProvider
import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import com.dosparta.trivia.domain.usecase.FinishGameUseCase
import com.dosparta.trivia.domain.usecase.LoadCategoriesUseCase
import com.dosparta.trivia.domain.usecase.StartGameSession
import com.dosparta.trivia.domain.usecase.SubmitAnswerUseCase
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import com.dosparta.trivia.ui.components.ErrorScreen
import com.dosparta.trivia.ui.viewmodel.TriviaViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class ScreenTests {
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    @get:Rule
    val composeTestRule = createComposeRule()

    private val dispatcher = StandardTestDispatcher()

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

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
    fun `TriviaScreen highlights incorrect and correct answers`() = runTest {
        val question = TriviaQuestion(
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
            override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> = listOf(question)

            override suspend fun getCategories(): List<TriviaCategory> = emptyList()
        }
        val viewModel = TriviaViewModel(
            startGame = StartGameSession(fakeRepo),
            submitAnswer = SubmitAnswerUseCase(GameEngine()),
            finishGame = FinishGameUseCase(GameEngine()),
            loadCategoriesUseCase = LoadCategoriesUseCase(fakeRepo),
            gameSessionRepository = fakeSessionRepo
        )

        viewModel.loadQuestions(TriviaConfig(amount = 10))
        advanceUntilIdle()

        composeTestRule.setContent {
            TriviaScreen(
                viewModel = viewModel,
                onResult = {}
            )
        }

        composeTestRule.onNodeWithText("Salt").performClick()

        composeTestRule.onNodeWithTag("answer_option_1").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "incorrect")
        )
        composeTestRule.onNodeWithTag("answer_option_0").assert(
            SemanticsMatcher.expectValue(SemanticsProperties.StateDescription, "correct")
        )
    }
}
