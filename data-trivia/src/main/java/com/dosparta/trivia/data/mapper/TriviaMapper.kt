package com.dosparta.trivia.data.mapper

import android.text.Html
import com.dosparta.trivia.data.remote.dto.TriviaCategoriesResponseDto
import com.dosparta.trivia.data.remote.dto.TriviaCategoryDto
import com.dosparta.trivia.data.remote.dto.TriviaQuestionDto
import com.dosparta.trivia.data.remote.dto.TriviaResponseDto
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion

object TriviaMapper {

    /** Map the entire response to a list of domain models */
    fun fromResponse(response: TriviaResponseDto): List<TriviaQuestion> {
        if (response.responseCode != 0) {
            throw IllegalStateException("Trivia API returned response_code=${response.responseCode}.")
        }
        if (response.results.isEmpty()) {
            throw IllegalStateException("Trivia API returned no questions.")
        }

        return response.results.map(::fromDto)
    }

    /** Map a single DTO to the domain model */
    fun fromDto(dto: TriviaQuestionDto): TriviaQuestion {
        val decodedCategory = htmlDecode(dto.category).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia question category is missing.")
        val decodedType = htmlDecode(dto.type).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia question type is missing.")
        val decodedDifficulty = htmlDecode(dto.difficulty).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia question difficulty is missing.")
        val decodedQuestion = htmlDecode(dto.question).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia question text is missing.")
        val decodedCorrect = htmlDecode(dto.correctAnswer).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia question correct answer is missing.")
        val decodedIncorrects = dto.incorrectAnswers.map { htmlDecode(it) }.filter { it.isNotBlank() }

        if (decodedIncorrects.isEmpty() && dto.incorrectAnswers.isNotEmpty()) {
            throw IllegalStateException("Trivia question incorrect answers are malformed.")
        }

        val allAnswers = (decodedIncorrects + decodedCorrect).shuffled()

        return TriviaQuestion(
            category = decodedCategory,
            type = decodedType,
            difficulty = decodedDifficulty,
            question = decodedQuestion,
            correctAnswer = decodedCorrect,
            options = allAnswers
        )
    }

    fun fromCategoriesResponse(response: TriviaCategoriesResponseDto): List<TriviaCategory> {
        return response.triviaCategories.map(::fromCategoryDto)
    }

    fun fromCategoryDto(dto: TriviaCategoryDto): TriviaCategory {
        val decodedName = htmlDecode(dto.name).takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia category name is missing.")

        return TriviaCategory(
            id = dto.id,
            name = decodedName
        )
    }

    /** Utility to turn HTML‐encoded strings into plain text */
    private fun htmlDecode(text: String): String =
        Html.fromHtml(text, Html.FROM_HTML_MODE_LEGACY).toString()
}
