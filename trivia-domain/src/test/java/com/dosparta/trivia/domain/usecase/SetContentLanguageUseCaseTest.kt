package com.dosparta.trivia.domain.usecase

import com.dosparta.trivia.domain.localization.IContentLocalizationRepository
import com.dosparta.trivia.domain.model.AppLanguage
import kotlinx.coroutines.flow.MutableStateFlow
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SetContentLanguageUseCaseTest {

    private val repository = object : IContentLocalizationRepository {
        override val contentLanguage = MutableStateFlow(AppLanguage.ENGLISH)
        override val translationUnavailable = MutableStateFlow(false)
        override fun setContentLanguage(language: AppLanguage) {
            contentLanguage.value = language
        }
        override fun setTranslationUnavailable(unavailable: Boolean) {
            translationUnavailable.value = unavailable
        }
    }
    private val useCase = SetContentLanguageUseCase(repository)

    @Test
    fun `returns true and updates when the language changes`() {
        assertTrue(useCase(AppLanguage.SPANISH))
        assertEquals(AppLanguage.SPANISH, repository.contentLanguage.value)
    }

    @Test
    fun `returns false when the language is unchanged`() {
        assertFalse(useCase(AppLanguage.ENGLISH))
    }
}
