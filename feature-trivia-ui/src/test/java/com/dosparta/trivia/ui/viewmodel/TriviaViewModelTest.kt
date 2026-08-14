package com.dosparta.trivia.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.usecase.FinishGameUseCase
import com.dosparta.trivia.domain.usecase.LoadCategoriesUseCase
import com.dosparta.trivia.domain.usecase.StartGameSession
import com.dosparta.trivia.domain.usecase.SubmitAnswerUseCase
import com.dosparta.trivia.ui.R
import com.dosparta.trivia.ui.UiText
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.delay
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaViewModelTest {

    // Make LiveData / StateFlow execute synchronously
    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    // Use the test dispatcher as Main
    private val dispatcher = StandardTestDispatcher()

    private lateinit var startGame: StartGameSession
    private lateinit var submitAnswer: SubmitAnswerUseCase
    private lateinit var finishGame: FinishGameUseCase
    private lateinit var loadCategoriesUseCase: LoadCategoriesUseCase
    private lateinit var gameSessionRepository: IGameSessionRepository
    private lateinit var viewModel: TriviaViewModel

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startGame = mockk()
        submitAnswer = mockk()
        finishGame = mockk()
        loadCategoriesUseCase = mockk()
        gameSessionRepository = mockk()
        coEvery { gameSessionRepository.getActiveSession() } returns null
        coEvery { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) } returns Unit
        coEvery { gameSessionRepository.clearActiveSession() } returns Unit
        viewModel = TriviaViewModel(startGame, submitAnswer, finishGame, loadCategoriesUseCase, gameSessionRepository)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `loadQuestions emits session on success`() = runTest {
        // given
        val session = GameSession(questions = listOf(), startTimeMillis = 42L)
        val config = TriviaConfig(amount = 10)
        coEvery { startGame.invoke(config) } returns session

        // when
        viewModel.loadQuestions(config)
        advanceUntilIdle()

        // then
        assertTrue(viewModel.uiState.value is TriviaUiState.Game)
        assertEquals(session, (viewModel.uiState.value as TriviaUiState.Game).session)
    }

    @Test
    fun `loadCategories emits available categories`() = runTest {
        val categories = listOf(
            TriviaCategory(9, "General Knowledge"),
            TriviaCategory(17, "Science & Nature")
        )
        coEvery { loadCategoriesUseCase.invoke() } returns categories

        viewModel.loadCategories()
        advanceUntilIdle()

        assertEquals(categories, viewModel.categories.value)
        assertEquals(null, viewModel.categoriesError.value)
    }

    @Test
    fun `loadCategories emits a user friendly error when fetch fails`() = runTest {
        coEvery { loadCategoriesUseCase.invoke() } throws RuntimeException("boom")

        viewModel.loadCategories()
        advanceUntilIdle()

        assertEquals(UiText.StringResource(R.string.error_loading_categories), viewModel.categoriesError.value)
    }

    @Test
    fun `loadQuestions emits error on failure`() = runTest {
        // given
        coEvery { startGame.invoke(any()) } throws RuntimeException("oops")

        // when
        viewModel.loadQuestions(10)
        advanceUntilIdle()

        // then
        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is TriviaUiState.Error)
        assertEquals(UiText.StringResource(R.string.error_unknown), (viewModel.uiState.value as TriviaUiState.Error).message)
    }

    @Test
    fun `submit emits GameResult when session ends`() = runTest {
        // 1. Prepare a session of size 1 that’s already “done”
        val finishedSession = GameSession(
            questions = listOf(TriviaQuestion(
                "General", "boolean", "easy",
                "Is sky blue?", "True", listOf("True", "False"))),
            currentIndex = 1,
            correctCount = 1,
            startTimeMillis = 0L
        )
        coEvery { startGame.invoke(any()) } returns finishedSession
        coEvery { submitAnswer.invoke(any(), any()) } returns finishedSession

        val expectedResult = GameResult(1, 1, 100L)
        coEvery { finishGame.invoke(finishedSession) } returns expectedResult

        viewModel.loadQuestions(TriviaConfig(amount = 10))
        advanceUntilIdle()

        viewModel.submit("True")
        advanceUntilIdle()

        advanceUntilIdle()
        assertTrue(viewModel.uiState.value is TriviaUiState.Result)
        assertEquals(expectedResult, (viewModel.uiState.value as TriviaUiState.Result).result)
    }

    @Test
    fun `loadQuestions ignores duplicate requests while a load is already in progress`() = runTest {
        val session = GameSession(
            questions = listOf(
                TriviaQuestion(
                    category = "General",
                    type = "boolean",
                    difficulty = "easy",
                    question = "Is Kotlin fun?",
                    correctAnswer = "True",
                    options = listOf("True", "False")
                )
            ),
            startTimeMillis = 42L
        )

        val config = TriviaConfig(amount = 10)
        coEvery { startGame.invoke(config) } coAnswers {
            delay(200)
            session
        }

        viewModel.loadQuestions(config)
        viewModel.loadQuestions(config)
        advanceUntilIdle()

        coVerify(exactly = 1) { startGame.invoke(config) }
        assertTrue(viewModel.uiState.value is TriviaUiState.Game)
    }
}
