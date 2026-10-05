package com.dosparta.trivia.domain.game

import com.dosparta.trivia.domain.model.TriviaQuestion
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class GameEngineTest {

    private lateinit var engine: GameEngine
    private lateinit var singleQuestionList: List<TriviaQuestion>

    @BeforeTest
    fun setup() {
        engine = GameEngine()
        // A sample question for testing
        val sample = TriviaQuestion(
            category = "Test",
            type = "multiple",
            difficulty = "easy",
            question = "What is 2 + 2?",
            correctAnswer = "4",
            options = listOf("4", "3", "5", "2")
        )
        singleQuestionList = listOf(sample)
    }

    @Test
    fun `submitAnswer increments correctCount when answer is correct`() {
        val session = GameSession(questions = singleQuestionList)
        val updated = engine.submitAnswer(session, "4")

        assertEquals(1, updated.currentIndex)
        assertEquals(1, updated.correctCount)
    }

    @Test
    fun `submitAnswer does not increment correctCount when answer is incorrect`() {
        val session = GameSession(questions = singleQuestionList)
        val updated = engine.submitAnswer(session, "3")

        assertEquals(1, updated.currentIndex)
        assertEquals(0, updated.correctCount)
    }

    @Test
    fun `submitAnswer ignores casing and extra whitespace in answer comparison`() {
        val session = GameSession(questions = singleQuestionList)
        val updated = engine.submitAnswer(session, " 4 ")

        assertEquals(1, updated.currentIndex)
        assertEquals(1, updated.correctCount)
    }

    @Test
    fun `submitAnswer returns session unchanged when no current question exists`() {
        val session = GameSession(questions = emptyList(), currentIndex = 0)
        val updated = engine.submitAnswer(session, "4")

        assertEquals(session, updated)
        assertNull(session.currentQuestion)
    }

    @Test
    fun `currentQuestion is null when index is out of range`() {
        val session = GameSession(questions = singleQuestionList, currentIndex = 10)

        assertNull(session.currentQuestion)
        assertFalse(engine.hasMoreQuestions(session))
    }

    @Test
    fun `submitAnswer treats blank answers as incorrect without crashing`() {
        val session = GameSession(questions = singleQuestionList)
        val updated = engine.submitAnswer(session, "   ")

        assertEquals(1, updated.currentIndex)
        assertEquals(0, updated.correctCount)
    }

    @Test
    fun `hasMoreQuestions returns false when at end of questions`() {
        val session = GameSession(questions = singleQuestionList, currentIndex = 1)
        assertFalse(engine.hasMoreQuestions(session))
    }

    @Test
    fun `hasMoreQuestions returns true when questions remain`() {
        val session = GameSession(questions = singleQuestionList, currentIndex = 0)
        assertTrue(engine.hasMoreQuestions(session))
    }

    @Test
    fun `finish returns correct GameResult`() {
        val session = GameSession(
            questions = singleQuestionList,
            currentIndex = 1,
            correctCount = 1,
            startTimeMillis = 0L
        )
        val result = engine.finish(session)

        assertEquals(1, result.totalQuestions)
        assertEquals(1, result.correctAnswers)
        assertTrue(result.durationMillis >= 0L)
        assertEquals(0, result.incorrectAnswers)
        assertEquals(100.0, result.scorePercentage, absoluteTolerance = 0.001)
    }

    @Test
    fun `formattedDuration includes hours when needed`() {
        val result = GameResult(
            totalQuestions = 2,
            correctAnswers = 1,
            durationMillis = 3_600_000L + 125_000L
        )

        assertEquals("1:02:05", result.formattedDuration())
    }
}
