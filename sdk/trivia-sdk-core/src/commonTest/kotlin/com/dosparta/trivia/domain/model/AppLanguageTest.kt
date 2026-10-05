package com.dosparta.trivia.domain.model

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

class AppLanguageTest {

    @Test
    fun `fromTag resolves supported languages regardless of region`() {
        assertEquals(AppLanguage.ENGLISH, AppLanguage.fromTag("en-GB"))
        assertEquals(AppLanguage.GERMAN, AppLanguage.fromTag("de-AT"))
        assertEquals(AppLanguage.SPANISH, AppLanguage.fromTag("es_419"))
    }

    @Test
    fun `fromTag maps only simplified chinese`() {
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, AppLanguage.fromTag("zh"))
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, AppLanguage.fromTag("zh-CN"))
        assertEquals(AppLanguage.CHINESE_SIMPLIFIED, AppLanguage.fromTag("zh-Hans-HK"))
        assertNull(AppLanguage.fromTag("zh-TW"))
        assertNull(AppLanguage.fromTag("zh-Hant"))
    }

    @Test
    fun `fromTag returns null for unsupported or missing tags`() {
        assertNull(AppLanguage.fromTag("fr-FR"))
        assertNull(AppLanguage.fromTag(""))
        assertNull(AppLanguage.fromTag(null))
    }

    @Test
    fun `tags round trip`() {
        AppLanguage.entries.forEach { language ->
            assertEquals(language, AppLanguage.fromTag(language.tag))
        }
    }

    @Test
    fun `only non english languages require translation`() {
        assertFalse(AppLanguage.ENGLISH.requiresTranslation)
        assertTrue(AppLanguage.GERMAN.requiresTranslation)
    }
}
