package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.game.GameEngine
import com.dosparta.trivia.domain.game.GameResult
import com.dosparta.trivia.domain.game.GameSession
import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlinx.coroutines.runBlocking
import io.mockk.MockKAnnotations
import io.mockk.every
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
class FinishGameUseCaseTest {

    @MockK
    private lateinit var engine: GameEngine

    private lateinit var finishUseCase: FinishGameUseCase

    private lateinit var singleQuestionList: List<TriviaQuestion>
    private lateinit var session: GameSession
    private lateinit var expectedResult: GameResult

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        finishUseCase = FinishGameUseCase(engine)

        val sample = TriviaQuestion(
            category = "Test",
            type = "multiple",
            difficulty = "easy",
            question = "Pick A?",
            correctAnswer = "A",
            options = listOf("A", "B", "C", "D")
        )
        singleQuestionList = listOf(sample)

        // Simulate a finished session
        session = GameSession(
            questions = singleQuestionList,
            currentIndex = 1,
            correctCount = 1,
            startTimeMillis = System.currentTimeMillis()
        )

        // Build an expected GameResult
        expectedResult = GameResult(
            totalQuestions = 1,
            correctAnswers = 1,
            durationMillis = 2_000L
        )

        every { engine.finish(any()) } returns expectedResult
    }

    @Test
    fun `invoke returns GameResult from engine`() = runBlocking {
        val result = finishUseCase(session)
        assertEquals(1, result.totalQuestions)
        assertEquals(1, result.correctAnswers)
        assertEquals(0, result.incorrectAnswers)
        assertEquals(100.0, result.scorePercentage, 0.001)
        // durationMillis is exactly 2000 because of the stub
        assertEquals(2_000L, result.durationMillis)
    }

    @Test(expected = RuntimeException::class)
    fun `invoke propagates exception from engine`() {
        runBlocking {
            every { engine.finish(any()) } throws RuntimeException("Engine crashed")

            // Should throw
            finishUseCase(session)
        }
    }
}
