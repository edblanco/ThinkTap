package com.dosparta.trivia.data.repository

import android.util.Log
import com.dosparta.trivia.data.localization.TriviaContentTranslator
import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Decorates the OpenTDB repository and translates its English content into the active
 * content language. If translation fails, the original English content is returned and
 * [IContentLocalizationRepository.translationUnavailable] is raised so the UI can show a notice.
 */
@Singleton
class TranslatingTriviaRepository @Inject constructor(
    private val delegate: TriviaRepositoryImpl,
    private val contentTranslator: TriviaContentTranslator,
    private val localization: IContentLocalizationRepository
) : ITriviaRepository {

    override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> {
        val questions = delegate.getQuestions(config)
        return translateOrFallback(questions) { target ->
            contentTranslator.translateQuestions(questions, target)
        }
    }

    override suspend fun getCategories(): List<TriviaCategory> {
        val categories = delegate.getCategories()
        return translateOrFallback(categories) { target ->
            contentTranslator.translateCategories(categories, target)
        }
    }

    private suspend fun <T> translateOrFallback(
        original: T,
        translate: suspend (AppLanguage) -> T
    ): T {
        val target = localization.contentLanguage.value
        if (!target.requiresTranslation) {
            localization.setTranslationUnavailable(false)
            return original
        }
        return try {
            translate(target).also { localization.setTranslationUnavailable(false) }
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Translation to ${target.tag} failed; falling back to English.", e)
            localization.setTranslationUnavailable(true)
            original
        }
    }

    private companion object {
        const val TAG = "TranslatingTriviaRepo"
    }
}
