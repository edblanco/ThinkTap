package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.StartupDecision
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.runBlocking
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
class AppStartupUseCasesTest {

    @MockK
    private lateinit var gameSessionRepository: IGameSessionRepository

    @MockK
    private lateinit var triviaRepository: ITriviaRepository

    private lateinit var resolveAppStartupUseCase: ResolveAppStartupUseCase
    private lateinit var persistGameSessionUseCase: PersistGameSessionUseCase
    private lateinit var clearGameSessionUseCase: ClearGameSessionUseCase
    private lateinit var loadCategoriesUseCase: LoadCategoriesUseCase

    @Before
    fun setUp() {
        MockKAnnotations.init(this, relaxUnitFun = true)
        resolveAppStartupUseCase = ResolveAppStartupUseCase(gameSessionRepository)
        persistGameSessionUseCase = PersistGameSessionUseCase(gameSessionRepository)
        clearGameSessionUseCase = ClearGameSessionUseCase(gameSessionRepository)
        loadCategoriesUseCase = LoadCategoriesUseCase(triviaRepository)
    }

    @Test
    fun `resolve app startup restores when an active session exists`() = runBlocking {
        val questions = listOf(
            TriviaQuestion(
                category = "Science",
                type = "multiple",
                difficulty = "easy",
                question = "What is H2O?",
                correctAnswer = "Water",
                options = listOf("Water", "Salt", "Sugar", "Ice")
            )
        )
        val session = GameSessionState(
            questions = questions,
            currentIndex = 0,
            correctCount = 1,
            activeElapsedMillis = 42_000L,
            selectedAnswers = mapOf(0 to "Water")
        )

        coEvery { gameSessionRepository.getActiveSession() } returns session

        val result = resolveAppStartupUseCase()

        assertTrue(result is StartupDecision.RestoreGame)
        assertEquals(session, (result as StartupDecision.RestoreGame).session)
    }

    @Test
    fun `resolve app startup loads categories when no active session exists`() = runBlocking {
        coEvery { gameSessionRepository.getActiveSession() } returns null

        val result = resolveAppStartupUseCase()

        assertEquals(StartupDecision.LoadCategories, result)
    }

    @Test
    fun `persist game session delegates all values to repository`() = runBlocking {
        val questions = listOf(
            TriviaQuestion(
                category = "History",
                type = "boolean",
                difficulty = "medium",
                question = "Was Rome founded in Italy?",
                correctAnswer = "True",
                options = listOf("True", "False")
            )
        )
        val selectedAnswers = mapOf(0 to "True")

        persistGameSessionUseCase(
            questions = questions,
            currentIndex = 1,
            correctCount = 1,
            activeElapsedMillis = 99_000L,
            selectedAnswers = selectedAnswers
        )

        coVerify { gameSessionRepository.saveGameSession(
            questions = questions,
            currentIndex = 1,
            correctCount = 1,
            activeElapsedMillis = 99_000L,
            selectedAnswers = selectedAnswers
        ) }
    }

    @Test
    fun `clear game session delegates removal to repository`() = runBlocking {
        clearGameSessionUseCase()

        coVerify { gameSessionRepository.clearActiveSession() }
    }

    @Test
    fun `load categories returns repository categories`() = runBlocking {
        val categories = listOf(
            TriviaCategory(id = 9, name = "General Knowledge"),
            TriviaCategory(id = 17, name = "Science & Nature")
        )
        coEvery { triviaRepository.getCategories() } returns categories

        val result = loadCategoriesUseCase()

        assertEquals(categories, result)
    }
}
