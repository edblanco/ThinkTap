package com.dosparta.trivia.data.localization.deepl

import android.util.Log
import com.dosparta.trivia.data.BuildConfig
import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cloud-based translation backed by the DeepL API.
 *
 * Uses thread-safe in-memory caching to avoid redundant API calls and conserve character quota.
 * Throws an exception when the API key is not configured, quota is exceeded, or network fails,
 * enabling automatic fallback to on-device translation.
 */
@Singleton
internal class DeepLTextTranslator @Inject constructor(
    private val api: DeepLApi,
    private val apiKey: String = BuildConfig.DEEPL_API_KEY
) : ITextTranslator {

    private val cache = ConcurrentHashMap<AppLanguage, MutableMap<String, String>>()

    override suspend fun translate(texts: List<String>, target: AppLanguage): List<String> {
        if (!target.requiresTranslation || texts.isEmpty()) return texts
        check(apiKey.isNotBlank()) { "DeepL API key is not configured." }

        val languageCache = cache.getOrPut(target) { ConcurrentHashMap() }
        val uncached = texts.filter { it.isNotBlank() && !languageCache.containsKey(it) }.distinct()

        if (uncached.isNotEmpty()) {
            Log.d(TAG, "Translating ${uncached.size} text(s) to ${target.tag} with DeepL.")
            val response = api.translate(
                authorization = "DeepL-Auth-Key $apiKey",
                request = DeepLTranslationRequest(
                    text = uncached,
                    targetLang = target.toDeepLLanguage()
                )
            )
            val translations = response.translations
            check(translations.size == uncached.size) {
                "DeepL returned ${translations.size} translations for ${uncached.size} texts."
            }
            uncached.zip(translations).forEach { (original, item) ->
                languageCache[original] = item.text
            }
            Log.d(TAG, "DeepL translation completed for ${uncached.size} text(s).")
        }

        return texts.map { text ->
            if (text.isBlank()) text else languageCache[text] ?: text
        }
    }

    private fun AppLanguage.toDeepLLanguage(): String = when (this) {
        AppLanguage.ENGLISH -> "EN"
        AppLanguage.GERMAN -> "DE"
        AppLanguage.SPANISH -> "ES"
        AppLanguage.CHINESE_SIMPLIFIED -> "ZH"
    }

    private companion object {
        const val TAG = "DeepLTextTranslator"
    }
}
