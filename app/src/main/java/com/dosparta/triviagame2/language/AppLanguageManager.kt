package com.dosparta.triviagame2.language

import androidx.appcompat.app.AppCompatDelegate
import androidx.core.os.LocaleListCompat
import com.dosparta.trivia.domain.model.AppLanguage

/**
 * Reads and applies the per-app language override through AppCompat, which persists the choice
 * (system-managed on Android 13+, `autoStoreLocales` below that) and recreates the activity.
 *
 * A `null` language means "follow the system language".
 */
class AppLanguageManager {

    fun currentOverride(): AppLanguage? {
        val locales = AppCompatDelegate.getApplicationLocales()
        if (locales.isEmpty) return null
        return AppLanguage.fromTag(locales[0]?.toLanguageTag())
    }

    fun applyOverride(language: AppLanguage?) {
        val locales = language?.let { LocaleListCompat.forLanguageTags(it.tag) }
            ?: LocaleListCompat.getEmptyLocaleList()
        AppCompatDelegate.setApplicationLocales(locales)
    }
}
