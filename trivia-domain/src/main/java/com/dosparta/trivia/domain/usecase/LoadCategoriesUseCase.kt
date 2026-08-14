package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.repository.ITriviaRepository
import javax.inject.Inject

class LoadCategoriesUseCase @Inject constructor(
    private val repository: ITriviaRepository
) {
    suspend operator fun invoke(): List<TriviaCategory> = repository.getCategories()
}
