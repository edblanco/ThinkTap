package com.dosparta.trivia.data.localization

import android.util.Log
import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Composite [ITextTranslator] that attempts translation with a high-accuracy primary engine
 * (such as DeepL) and silently falls back to a secondary engine (such as on-device ML Kit)
 * if the primary engine is unconfigured, rate-limited, offline, or fails.
 */
@Singleton
internal class DualEngineTextTranslator @Inject constructor(
    private val primary: ITextTranslator,
    private val fallback: ITextTranslator
) : ITextTranslator {

    override suspend fun translate(texts: List<String>, target: AppLanguage): List<String> {
        if (!target.requiresTranslation || texts.isEmpty()) return texts

        return try {
            primary.translate(texts, target)
        } catch (e: CancellationException) {
            throw e
        } catch (e: Exception) {
            Log.w(TAG, "Primary translation engine failed; falling back to secondary engine.", e)
            fallback.translate(texts, target)
        }
    }

    private companion object {
        const val TAG = "DualEngineTranslator"
    }
}
