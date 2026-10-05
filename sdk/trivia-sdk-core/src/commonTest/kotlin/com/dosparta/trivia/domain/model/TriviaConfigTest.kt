package com.dosparta.trivia.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFailsWith

class TriviaConfigTest {

    @Test
    fun `valid config accepts allowed amount and difficulty`() {
        val config = TriviaConfig(
            amount = 20,
            categoryId = 9,
            difficulty = "easy"
        )

        assertEquals(20, config.amount)
        assertEquals(9, config.categoryId)
        assertEquals("easy", config.normalizedDifficulty())
    }

    @Test
    fun `mixed difficulty maps to null for the request URL`() {
        val config = TriviaConfig(amount = 10, difficulty = "mixed")

        assertEquals(null, config.normalizedDifficulty())
    }

    @Test
    fun `invalid amount is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            TriviaConfig(amount = 9)
        }

        assertEquals("Amount must be between 10 and 50.", exception.message)
    }

    @Test
    fun `invalid difficulty is rejected`() {
        val exception = assertFailsWith<IllegalArgumentException> {
            TriviaConfig(amount = 10, difficulty = "expert")
        }

        assertEquals("Difficulty must be one of easy, medium, hard, mixed.", exception.message)
    }
}
