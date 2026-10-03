package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.runBlocking
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
class StartGameSessionTest {

    @MockK
    private lateinit var repo: ITriviaRepository

    private lateinit var startSession: StartGameSession
    private lateinit var sampleList: List<TriviaQuestion>
    private lateinit var config: TriviaConfig

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val sample = TriviaQuestion(
            category = "General",
            type = "boolean",
            difficulty = "medium",
            question = "Is the sky blue?",
            correctAnswer = "True",
            options = listOf("True", "False")
        )
        sampleList = listOf(sample)
        config = TriviaConfig(amount = 10, categoryId = 9, difficulty = "easy")

        runBlocking {
            coEvery { repo.getQuestions(any<TriviaConfig>()) } returns sampleList
        }

        startSession = StartGameSession(repo)
    }

    @Test
    fun `invoke produces GameSession with correct fields`() = runBlocking {
        val beforeTime = System.currentTimeMillis()
        val session = startSession(config)

        assertEquals(1, session.questions.size)
        assertEquals("Is the sky blue?", session.questions[0].question)
        assertEquals(0, session.currentIndex)
        assertEquals(0, session.correctCount)
        assertTrue(session.startTimeMillis >= beforeTime)
        assertEquals("Is the sky blue?", session.currentQuestion?.question)
    }

    @Test
    fun `invoke propagates exception from repository`() = runBlocking {
        coEvery { repo.getQuestions(any<TriviaConfig>()) } throws RuntimeException("API down")

        try {
            startSession(config)
            fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals("API down", e.message)
        }
    }
}
