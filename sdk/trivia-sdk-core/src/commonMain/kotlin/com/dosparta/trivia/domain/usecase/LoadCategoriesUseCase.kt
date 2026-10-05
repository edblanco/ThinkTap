package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.repository.ITriviaRepository

internal class LoadCategoriesUseCase(
    private val repository: ITriviaRepository
) {
    suspend operator fun invoke(): List<TriviaCategory> = repository.getCategories()
}
