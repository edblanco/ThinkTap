package com.dosparta.trivia.domain.repository

import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion

/**
 * Abstraction over where and how trivia questions and categories are loaded.
 */
interface ITriviaRepository {
    /**
     * Fetches a valid trivia session using the given configuration.
     * @throws Exception on network, validation, or mapping errors.
     */
    suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion>

    /**
     * Fetches all available categories from the trivia backend.
     * @throws Exception on network or mapping errors.
     */
    suspend fun getCategories(): List<TriviaCategory>
}