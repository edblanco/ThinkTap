package com.dosparta.trivia.data.localization

import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaQuestion
import javax.inject.Inject

/**
 * Translates trivia domain models while keeping answer matching consistent.
 *
 * Every distinct string is translated exactly once, and both `options` and `correctAnswer` are
 * rebuilt from the same lookup, so the correct answer always matches one of the options.
 * Boolean questions keep their "True"/"False" answers untouched — the UI localizes them from
 * string resources, which is more reliable than machine translation for single words.
 */
internal class TriviaContentTranslator @Inject constructor(
    private val translator: ITextTranslator
) {
    suspend fun translateQuestions(
        questions: List<TriviaQuestion>,
        target: AppLanguage
    ): List<TriviaQuestion> {
        if (!target.requiresTranslation || questions.isEmpty()) return questions

        val sourceTexts = questions.flatMap { question ->
            buildList {
                add(question.category)
                add(question.question)
                if (!question.isBoolean) addAll(question.options + question.correctAnswer)
            }
        }.distinct()
        val lookup = sourceTexts.zip(translator.translate(sourceTexts, target)).toMap()

        return questions.map { question -> question.translatedWith(lookup) }
    }

    suspend fun translateCategories(
        categories: List<TriviaCategory>,
        target: AppLanguage
    ): List<TriviaCategory> {
        if (!target.requiresTranslation || categories.isEmpty()) return categories

        val translatedNames = translator.translate(categories.map { it.name }, target)
        return categories.zip(translatedNames) { category, name -> category.copy(name = name) }
    }

    private fun TriviaQuestion.translatedWith(lookup: Map<String, String>): TriviaQuestion {
        val translatedBase = copy(
            category = lookup.getValue(category),
            question = lookup.getValue(question)
        )
        if (isBoolean) return translatedBase

        val translatedOptions = options.map { lookup.getValue(it) }
        // Distinct English answers can collapse into one translation (e.g. "Tomato" / "tomato");
        // keep English answers then, so the player can still tell options apart.
        val optionsStayDistinct = translatedOptions.toSet().size == options.toSet().size
        return if (optionsStayDistinct) {
            translatedBase.copy(
                correctAnswer = lookup.getValue(correctAnswer),
                options = translatedOptions
            )
        } else {
            translatedBase
        }
    }

    private val TriviaQuestion.isBoolean: Boolean
        get() = type.equals(BOOLEAN_TYPE, ignoreCase = true)

    private companion object {
        const val BOOLEAN_TYPE = "boolean"
    }
}
