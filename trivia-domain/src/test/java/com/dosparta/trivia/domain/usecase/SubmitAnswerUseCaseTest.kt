package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.runBlocking
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
class SubmitAnswerUseCaseTest {

    @MockK
    private lateinit var engine: GameEngine

    private lateinit var submitUseCase: SubmitAnswerUseCase

    private lateinit var singleQuestionList: List<TriviaQuestion>
    private lateinit var initialSession: GameSession
    private lateinit var updatedSessionCorrect: GameSession
    private lateinit var updatedSessionWrong: GameSession

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        submitUseCase = SubmitAnswerUseCase(engine)

        val sample = TriviaQuestion(
            category = "Test",
            type = "multiple",
            difficulty = "easy",
            question = "Pick A?",
            correctAnswer = "A",
            options = listOf("A", "B", "C", "D")
        )
        singleQuestionList = listOf(sample)
        initialSession = GameSession(questions = singleQuestionList)

        // Build two expected outcomes
        updatedSessionCorrect = GameSession(
            questions = singleQuestionList,
            currentIndex = 1,
            correctCount = 1,
            startTimeMillis = initialSession.startTimeMillis
        )
        updatedSessionWrong = GameSession(
            questions = singleQuestionList,
            currentIndex = 1,
            correctCount = 0,
            startTimeMillis = initialSession.startTimeMillis
        )

        // STUB engine.submitAnswer without calling it:
        every { engine.submitAnswer(eq(initialSession), eq("A")) } returns updatedSessionCorrect

        every { engine.submitAnswer(eq(initialSession), eq("B")) } returns updatedSessionWrong
    }

    @Test
    fun `invoke returns updated session when correct answer`() = runBlocking {
        val result = submitUseCase(initialSession, "A")
        assertEquals(updatedSessionCorrect.currentIndex, result.currentIndex)
        assertEquals(updatedSessionCorrect.correctCount, result.correctCount)
    }

    @Test
    fun `invoke returns updated session when incorrect answer`() = runBlocking {
        val result = submitUseCase(initialSession, "B")
        assertEquals(1, result.currentIndex)
        assertEquals(0, result.correctCount)
    }

    @Test(expected = RuntimeException::class)
    fun `invoke propagates exception from engine`() {
        runBlocking {
            every { engine.submitAnswer(any(), any()) } throws RuntimeException("Engine failure")

            // Should throw
            submitUseCase(initialSession, "X")
        }
    }
}
