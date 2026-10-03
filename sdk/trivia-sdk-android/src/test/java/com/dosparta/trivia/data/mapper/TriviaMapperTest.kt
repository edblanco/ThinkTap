package com.dosparta.trivia.data.mapper

import android.os.Build
import com.dosparta.trivia.data.remote.dto.TriviaCategoriesResponseDto
import com.dosparta.trivia.data.remote.dto.TriviaCategoryDto
import com.dosparta.trivia.data.remote.dto.TriviaQuestionDto
import com.dosparta.trivia.data.remote.dto.TriviaResponseDto
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaMapperTest {

    @Test
    fun `fromDto maps TriviaQuestionDto to TriviaQuestion correctly`() {
        val dto = TriviaQuestionDto(
            type = "multiple",
            difficulty = "easy",
            category = "General Knowledge",
            question = "What is 2 + 2?",
            correctAnswer = "4",
            incorrectAnswers = listOf("3", "5", "2")
        )

        val domain = TriviaMapper.fromDto(dto)

        // Verify basic fields
        assertEquals("General Knowledge", domain.category)
        assertEquals("multiple", domain.type)
        assertEquals("easy", domain.difficulty)
        assertEquals("What is 2 + 2?", domain.question)
        assertEquals("4", domain.correctAnswer)

        // Options should contain all answers (correct + incorrect)
        val options = domain.options
        assertTrue(options.containsAll(listOf("4", "3", "5", "2")))
        assertEquals(4, options.size)
    }

    @Test
    fun `fromResponse maps TriviaResponseDto to list of TriviaQuestion`() {
        val dto1 = TriviaQuestionDto(
            type = "boolean",
            difficulty = "medium",
            category = "Science",
            question = "Is the earth round?",
            correctAnswer = "True",
            incorrectAnswers = listOf("False")
        )
        val dto2 = TriviaQuestionDto(
            type = "multiple",
            difficulty = "hard",
            category = "History",
            question = "Who was first president of USA?",
            correctAnswer = "George Washington",
            incorrectAnswers = listOf("Abraham Lincoln", "Thomas Jefferson", "John Adams")
        )
        val responseDto = TriviaResponseDto(
            responseCode = 0,
            results = listOf(dto1, dto2)
        )

        val domainList = TriviaMapper.fromResponse(responseDto)
        assertEquals(2, domainList.size)

        val first = domainList[0]
        assertEquals("Science", first.category)
        assertEquals("boolean", first.type)
        assertEquals("medium", first.difficulty)
        assertEquals("Is the earth round?", first.question)
        assertEquals("True", first.correctAnswer)
        assertTrue(first.options.containsAll(listOf("True", "False")))
        assertEquals(2, first.options.size)

        val second = domainList[1]
        assertEquals("History", second.category)
        assertEquals("multiple", second.type)
        assertEquals("hard", second.difficulty)
        assertEquals("Who was first president of USA?", second.question)
        assertEquals("George Washington", second.correctAnswer)
        assertTrue(second.options.containsAll(listOf("George Washington", "Abraham Lincoln", "Thomas Jefferson", "John Adams")))
        assertEquals(4, second.options.size)
    }

    @Test
    fun `fromCategoriesResponse maps categories list correctly`() {
        val response = TriviaCategoriesResponseDto(
            triviaCategories = listOf(
                TriviaCategoryDto(id = 9, name = "General Knowledge"),
                TriviaCategoryDto(id = 17, name = "Science &amp; Nature")
            )
        )

        val categories = TriviaMapper.fromCategoriesResponse(response)

        assertEquals(2, categories.size)
        assertEquals(9, categories[0].id)
        assertEquals("General Knowledge", categories[0].name)
        assertEquals(17, categories[1].id)
        assertEquals("Science & Nature", categories[1].name)
    }

    @Test
    fun `fromResponse rejects empty result lists explicitly`() {
        val response = TriviaResponseDto(
            responseCode = 0,
            results = emptyList()
        )

        val exception = assertThrows(TriviaSdkException::class.java) {
            TriviaMapper.fromResponse(response)
        }

        assertEquals(TriviaError.NO_QUESTIONS, exception.error)
    }

    @Test
    fun `fromDto rejects missing required fields`() {
        val dto = TriviaQuestionDto(
            type = "",
            difficulty = "easy",
            category = "General Knowledge",
            question = "What is 2 + 2?",
            correctAnswer = "4",
            incorrectAnswers = listOf("3")
        )

        val exception = assertThrows(TriviaSdkException::class.java) {
            TriviaMapper.fromDto(dto)
        }

        assertEquals(TriviaError.PROTOCOL, exception.error)
    }
}
