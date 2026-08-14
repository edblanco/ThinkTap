package com.dosparta.trivia.ui.viewmodel

import androidx.arch.core.executor.testing.InstantTaskExecutorRule
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
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
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import kotlin.math.abs
import kotlin.test.assertEquals
import kotlin.test.assertTrue

@OptIn(ExperimentalCoroutinesApi::class)
class TriviaViewModelPersistenceTest {

    @get:Rule
    val instantExecutorRule = InstantTaskExecutorRule()

    private val dispatcher = StandardTestDispatcher()

    private lateinit var startGame: StartGameSession
    private lateinit var submitAnswer: SubmitAnswerUseCase
    private lateinit var finishGame: FinishGameUseCase
    private lateinit var loadCategoriesUseCase: LoadCategoriesUseCase
    private lateinit var gameSessionRepository: IGameSessionRepository
    private lateinit var viewModel: TriviaViewModel

    private val question = TriviaQuestion(
        category = "Science",
        type = "multiple",
        difficulty = "easy",
        question = "What is H2O?",
        correctAnswer = "Water",
        options = listOf("Water", "Salt", "Sugar", "Ice")
    )

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        startGame = mockk()
        submitAnswer = mockk()
        finishGame = mockk()
        loadCategoriesUseCase = mockk()
        gameSessionRepository = mockk()

        coEvery { gameSessionRepository.getActiveSession() } returns null
        coEvery { loadCategoriesUseCase.invoke() } returns emptyList()
        coEvery { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) } returns Unit
        coEvery { gameSessionRepository.clearActiveSession() } returns Unit

        viewModel = TriviaViewModel(
            startGame = startGame,
            submitAnswer = submitAnswer,
            finishGame = finishGame,
            loadCategoriesUseCase = loadCategoriesUseCase,
            gameSessionRepository = gameSessionRepository
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `bootstrapApp navigates to trivia when persisted game exists`() = runTest {
        coEvery { gameSessionRepository.getActiveSession() } returns GameSessionState(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            activeElapsedMillis = 42L,
            selectedAnswers = emptyMap()
        )

        viewModel.bootstrapApp()
        advanceUntilIdle()

        assertTrue(viewModel.uiState.value is TriviaUiState.Game)
        assertEquals(StartupUiState.NavigateToTrivia, viewModel.startupState.value)
        coVerify(exactly = 0) { loadCategoriesUseCase.invoke() }
    }

    @Test
    fun `bootstrapApp loads categories and navigates to setup when no persisted game`() = runTest {
        val categories = listOf(TriviaCategory(9, "General Knowledge"))
        coEvery { loadCategoriesUseCase.invoke() } returns categories

        viewModel.bootstrapApp()
        advanceUntilIdle()

        assertEquals(categories, viewModel.categories.value)
        assertEquals(StartupUiState.NavigateToSetup, viewModel.startupState.value)
    }

    @Test
    fun `bootstrapApp emits startup error when category loading fails`() = runTest {
        coEvery { loadCategoriesUseCase.invoke() } throws RuntimeException("boom")

        viewModel.bootstrapApp()
        advanceUntilIdle()

        assertEquals(
            StartupUiState.Error(UiText.StringResource(R.string.error_loading_categories)),
            viewModel.startupState.value
        )
    }

    @Test
    fun `retryBootstrap retries startup flow`() = runTest {
        coEvery { loadCategoriesUseCase.invoke() } throws RuntimeException("boom") andThen listOf(
            TriviaCategory(12, "Music")
        )

        viewModel.bootstrapApp()
        advanceUntilIdle()
        assertTrue(viewModel.startupState.value is StartupUiState.Error)

        viewModel.retryBootstrap()
        advanceUntilIdle()
        assertEquals(StartupUiState.NavigateToSetup, viewModel.startupState.value)
    }

    @Test
    fun `loadQuestions saves session`() = runTest {
        val session = GameSession(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            startTimeMillis = 100L
        )
        coEvery { startGame.invoke(any()) } returns session

        viewModel.loadQuestions(10)
        advanceUntilIdle()

        coVerify {
            gameSessionRepository.saveGameSession(
                questions = session.questions,
                currentIndex = 0,
                correctCount = 0,
                activeElapsedMillis = any(),
                selectedAnswers = emptyMap()
            )
        }
    }

    @Test
    fun `submit clears persisted session when game finishes`() = runTest {
        val currentSession = GameSession(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            startTimeMillis = 100L
        )
        val finishedSession = currentSession.copy(currentIndex = 1, correctCount = 1)
        coEvery { startGame.invoke(any()) } returns currentSession
        coEvery { submitAnswer.invoke(any(), any()) } returns finishedSession
        coEvery { finishGame.invoke(any()) } returns GameResult(1, 1, 10)

        viewModel.loadQuestions(10)
        advanceUntilIdle()
        viewModel.submit("Water")
        advanceUntilIdle()

        coVerify { gameSessionRepository.clearActiveSession() }
    }

    @Test
    fun `saveGameOnPause saves when in game state`() = runTest {
        val session = GameSession(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            startTimeMillis = 100L
        )
        coEvery { startGame.invoke(any()) } returns session

        viewModel.loadQuestions(10)
        advanceUntilIdle()
        viewModel.saveGameOnPause()
        advanceUntilIdle()

        coVerify(atLeast = 1) {
            gameSessionRepository.saveGameSession(any(), any(), any(), any(), any())
        }
    }

    @Test
    fun `onAppResumed rebases timer so background time is not counted`() = runTest {
        val capturedElapsed = mutableListOf<Long>()
        coEvery { gameSessionRepository.saveGameSession(any(), any(), any(), any(), any()) } answers {
            capturedElapsed += args[3] as Long
            Unit
        }

        val session = GameSession(
            questions = listOf(question),
            currentIndex = 0,
            correctCount = 0,
            startTimeMillis = System.currentTimeMillis() - 2_000L
        )
        coEvery { startGame.invoke(any()) } returns session

        viewModel.loadQuestions(10)
        advanceUntilIdle()

        viewModel.saveGameOnPause()
        Thread.sleep(120L)
        viewModel.onAppResumed()
        viewModel.saveGameOnPause()
        advanceUntilIdle()

        val firstPauseElapsed = capturedElapsed[capturedElapsed.lastIndex - 1]
        val secondPauseElapsed = capturedElapsed.last()
        assertTrue(abs(secondPauseElapsed - firstPauseElapsed) < 80L)
    }
}
