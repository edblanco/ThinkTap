package com.dosparta.trivia.ui

import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository

/**
 * A fake TriviaRepository for tests.
 *
 * @param questions the list to return on getQuestions
 * @param error optional exception to throw instead of returning
 */
class FakeTriviaRepository(
    private var questions: List<TriviaQuestion> = emptyList(),
    private var error: Throwable? = null,
    private var categories: List<TriviaCategory> = emptyList()
) : ITriviaRepository {
    override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> {
        error?.let { throw it }
        return questions
    }

    override suspend fun getCategories(): List<TriviaCategory> = categories
}
