package com.dosparta.trivia.ui

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeContentLocalizationRepository(
    initialLanguage: AppLanguage = AppLanguage.ENGLISH
) : IContentLocalizationRepository {
    override val contentLanguage = MutableStateFlow(initialLanguage)
    override val translationUnavailable = MutableStateFlow(false)

    override fun setContentLanguage(language: AppLanguage) {
        contentLanguage.value = language
    }

    override fun setTranslationUnavailable(unavailable: Boolean) {
        translationUnavailable.value = unavailable
    }
}
