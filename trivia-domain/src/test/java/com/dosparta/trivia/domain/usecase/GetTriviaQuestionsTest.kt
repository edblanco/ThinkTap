package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.junit.MockitoJUnitRunner
import org.mockito.kotlin.any
import org.mockito.kotlin.whenever

@RunWith(MockitoJUnitRunner::class)
class GetTriviaQuestionsTest {

    @Mock
    private lateinit var repo: ITriviaRepository

    private lateinit var getTrivia: GetTriviaQuestions
    private lateinit var sampleConfig: TriviaConfig
    private lateinit var sampleList: List<TriviaQuestion>

    @Before
    fun setUp() {
        // Create a single sample TriviaQuestion
        val sample = TriviaQuestion(
            category = "General",
            type = "multiple",
            difficulty = "easy",
            question = "What is 2 + 2?",
            correctAnswer = "4",
            options = listOf("4", "3", "5", "2")
        )
        sampleList = listOf(sample)
        sampleConfig = TriviaConfig(amount = 10)

        runBlocking {
            whenever(repo.getQuestions(any<TriviaConfig>())).thenReturn(sampleList)
        }

        getTrivia = GetTriviaQuestions(repo)
    }

    @Test
    fun `invoke returns list from repository`() = runBlocking {
        val result = getTrivia(sampleConfig)
        assertEquals(1, result.size)
        assertEquals("What is 2 + 2?", result[0].question)
        assertEquals("4", result[0].correctAnswer)
    }

    @Test
    fun `invoke propagates exception from repository`() = runBlocking {
        whenever(repo.getQuestions(any<TriviaConfig>())).thenThrow(RuntimeException("Network error"))

        try {
            getTrivia(sampleConfig)
            // If we reach here, test should fail
            Assert.fail("Expected exception was not thrown")
        } catch (e: Exception) {
            assertEquals("Network error", e.message)
        }
    }
}
