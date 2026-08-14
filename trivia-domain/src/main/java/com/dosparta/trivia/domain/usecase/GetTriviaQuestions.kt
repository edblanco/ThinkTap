package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import javax.inject.Inject

/**
 * Use-case for retrieving trivia questions.
 */
class GetTriviaQuestions @Inject constructor(
    private val repository: ITriviaRepository
) {
    /**
     * Invoke to load questions.
     * @param config question selection configuration
     * @return a list of domain TriviaQuestion models
     */
    suspend operator fun invoke(config: TriviaConfig): List<TriviaQuestion> =
        repository.getQuestions(config)
}