package com.dosparta.trivia.data.localization

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory holder for the content language. The UI layer pushes the language resolved from the
 * active app locale, so the value is always in sync with the language the UI is rendered in.
 */
@Singleton
class ContentLocalizationRepositoryImpl @Inject constructor() : IContentLocalizationRepository {
    private val _contentLanguage = MutableStateFlow(AppLanguage.DEFAULT)
    override val contentLanguage: StateFlow<AppLanguage> = _contentLanguage.asStateFlow()

    private val _translationUnavailable = MutableStateFlow(false)
    override val translationUnavailable: StateFlow<Boolean> = _translationUnavailable.asStateFlow()

    override fun setContentLanguage(language: AppLanguage) {
        _contentLanguage.value = language
    }

    override fun setTranslationUnavailable(unavailable: Boolean) {
        _translationUnavailable.value = unavailable
    }
}
