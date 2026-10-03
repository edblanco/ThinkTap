package com.dosparta.trivia.data.remote

import com.dosparta.trivia.data.remote.dto.TriviaResponseDto
import com.squareup.moshi.Moshi
import com.squareup.moshi.kotlin.reflect.KotlinJsonAdapterFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

class TriviaDtoSerializationTest {
    @Test
    fun `transport DTOs deserialize without generated Moshi adapters`() {
        val moshi = Moshi.Builder().add(KotlinJsonAdapterFactory()).build()
        val response = moshi.adapter(TriviaResponseDto::class.java).fromJson(
            """
            {
              "response_code": 0,
              "results": [{
                "type": "multiple",
                "difficulty": "easy",
                "category": "Science",
                "question": "What is H2O?",
                "correct_answer": "Water",
                "incorrect_answers": ["Salt", "Sugar", "Ice"]
              }]
            }
            """.trimIndent()
        )
        assertNotNull(response)
        assertEquals(0, response?.responseCode)
        assertEquals("Water", response?.results?.single()?.correctAnswer)
    }
}
