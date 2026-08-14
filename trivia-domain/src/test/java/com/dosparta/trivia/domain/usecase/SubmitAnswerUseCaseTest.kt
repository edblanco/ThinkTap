package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.eq
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class SubmitAnswerUseCaseTest {

    @Mock
    private lateinit var engine: GameEngine

    private lateinit var submitUseCase: SubmitAnswerUseCase

    private lateinit var singleQuestionList: List<TriviaQuestion>
    private lateinit var initialSession: GameSession
    private lateinit var updatedSessionCorrect: GameSession
    private lateinit var updatedSessionWrong: GameSession

    @Before
    fun setUp() {
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
        whenever(engine.submitAnswer(eq(initialSession), eq("A")))
            .thenReturn(updatedSessionCorrect)

        whenever(engine.submitAnswer(eq(initialSession), eq("B")))
            .thenReturn(updatedSessionWrong)
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
            whenever(engine.submitAnswer(any(), any()))
                .thenThrow(RuntimeException("Engine failure"))

            // Should throw
            submitUseCase(initialSession, "X")
        }
    }
}
