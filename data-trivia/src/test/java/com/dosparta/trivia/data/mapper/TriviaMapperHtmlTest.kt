package com.dosparta.trivia.data.mapper

import android.os.Build
import com.dosparta.trivia.data.remote.dto.TriviaQuestionDto
import org.junit.Assert.assertEquals
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaMapperHtmlTest {

    @Test
    fun `fromDto decodes HTML entities correctly`() {
        // This DTO contains HTML‐encoded text: &quot; -> ", &amp; -> &
        val dto = TriviaQuestionDto(
            type = "multiple",
            difficulty = "easy",
            category = "General &amp; Knowledge",
            question = "Which symbol is called &quot;ampersand&quot;?",
            correctAnswer = "&amp;",
            incorrectAnswers = listOf("&lt;", "&gt;", "&quot;")
        )

        val domain = TriviaMapper.fromDto(dto)

        // After decoding:
        assertEquals("General & Knowledge", domain.category)
        assertEquals("Which symbol is called \"ampersand\"?", domain.question)
        assertEquals("&", domain.correctAnswer)

        // The options list should include decoded values for each
        val expectedOptions = listOf("&", "<", ">", "\"")
        assertEquals(4, domain.options.size)
        // Convert to a Set for easier contains‐check
        assertEquals(expectedOptions.toSet(), domain.options.toSet())
    }
}
