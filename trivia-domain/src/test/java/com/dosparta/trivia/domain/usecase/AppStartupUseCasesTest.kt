package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.StartupDecision
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.dosparta.trivia.domain.repository.IGameSessionRepository
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class AppStartupUseCasesTest {

    @Mock
    private lateinit var gameSessionRepository: IGameSessionRepository

    @Mock
    private lateinit var triviaRepository: ITriviaRepository

    private lateinit var resolveAppStartupUseCase: ResolveAppStartupUseCase
    private lateinit var persistGameSessionUseCase: PersistGameSessionUseCase
    private lateinit var clearGameSessionUseCase: ClearGameSessionUseCase
    private lateinit var loadCategoriesUseCase: LoadCategoriesUseCase

    @Before
    fun setUp() {
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

        whenever(gameSessionRepository.getActiveSession()).thenReturn(session)

        val result = resolveAppStartupUseCase()

        assertTrue(result is StartupDecision.RestoreGame)
        assertEquals(session, (result as StartupDecision.RestoreGame).session)
    }

    @Test
    fun `resolve app startup loads categories when no active session exists`() = runBlocking {
        whenever(gameSessionRepository.getActiveSession()).thenReturn(null)

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

        verify(gameSessionRepository).saveGameSession(
            questions = questions,
            currentIndex = 1,
            correctCount = 1,
            activeElapsedMillis = 99_000L,
            selectedAnswers = selectedAnswers
        )
    }

    @Test
    fun `clear game session delegates removal to repository`() = runBlocking {
        clearGameSessionUseCase()

        verify(gameSessionRepository).clearActiveSession()
    }

    @Test
    fun `load categories returns repository categories`() = runBlocking {
        val categories = listOf(
            TriviaCategory(id = 9, name = "General Knowledge"),
            TriviaCategory(id = 17, name = "Science & Nature")
        )
        whenever(triviaRepository.getCategories()).thenReturn(categories)

        val result = loadCategoriesUseCase()

        assertEquals(categories, result)
    }
}
