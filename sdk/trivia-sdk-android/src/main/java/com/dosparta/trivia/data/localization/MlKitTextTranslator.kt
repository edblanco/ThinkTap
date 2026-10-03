package com.dosparta.trivia.data.localization

import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import com.google.android.gms.tasks.Task
import com.google.mlkit.common.model.DownloadConditions
import com.google.mlkit.nl.translate.TranslateLanguage
import com.google.mlkit.nl.translate.Translation
import com.google.mlkit.nl.translate.Translator
import com.google.mlkit.nl.translate.TranslatorOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.util.concurrent.ConcurrentHashMap
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * On-device translation backed by ML Kit.
 *
 * Each target language needs a ~30 MB model that is downloaded on first use (any network).
 * Translators live for the whole process and results are memoized, so repeated category names
 * and answers are only translated once.
 */
@Singleton
internal class MlKitTextTranslator @Inject constructor() : ITextTranslator {

    private val translators = ConcurrentHashMap<AppLanguage, Translator>()
    private val readyLanguages = ConcurrentHashMap.newKeySet<AppLanguage>()
    private val cache = ConcurrentHashMap<AppLanguage, MutableMap<String, String>>()
    private val downloadMutex = Mutex()

    override suspend fun translate(texts: List<String>, target: AppLanguage): List<String> {
        if (!target.requiresTranslation || texts.isEmpty()) return texts

        val translator = ensureModelReady(target)
        val languageCache = cache.getOrPut(target) { ConcurrentHashMap() }
        return texts.map { text ->
            if (text.isBlank()) {
                text
            } else {
                languageCache[text] ?: translator.translate(text).await().also { languageCache[text] = it }
            }
        }
    }

    private suspend fun ensureModelReady(target: AppLanguage): Translator {
        // getOrPut on ConcurrentMap can create losing clients that would never be closed.
        val translator = translators.computeIfAbsent(target) {
            Translation.getClient(
                TranslatorOptions.Builder()
                    .setSourceLanguage(TranslateLanguage.ENGLISH)
                    .setTargetLanguage(target.toMlKitLanguage())
                    .build()
            )
        }
        if (target in readyLanguages) return translator

        downloadMutex.withLock {
            if (target !in readyLanguages) {
                // withTimeoutOrNull keeps a slow download from surfacing as a CancellationException,
                // which callers would otherwise treat as the whole load being cancelled.
                withTimeoutOrNull(MODEL_DOWNLOAD_TIMEOUT_MILLIS) {
                    translator.downloadModelIfNeeded(DownloadConditions.Builder().build()).await()
                    true
                } ?: error("Timed out downloading the ${target.tag} translation model.")
                readyLanguages += target
            }
        }
        return translator
    }

    private fun AppLanguage.toMlKitLanguage(): String = when (this) {
        AppLanguage.ENGLISH -> TranslateLanguage.ENGLISH
        AppLanguage.GERMAN -> TranslateLanguage.GERMAN
        AppLanguage.SPANISH -> TranslateLanguage.SPANISH
        AppLanguage.CHINESE_SIMPLIFIED -> TranslateLanguage.CHINESE
    }

    private suspend fun <T> Task<T>.await(): T = suspendCancellableCoroutine { continuation ->
        addOnSuccessListener { result -> continuation.resume(result) }
        addOnFailureListener { error -> continuation.resumeWithException(error) }
        addOnCanceledListener { continuation.cancel() }
    }

    private companion object {
        const val MODEL_DOWNLOAD_TIMEOUT_MILLIS = 60_000L
    }
}
