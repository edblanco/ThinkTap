package com.dosparta.trivia.data.localization.deepl

import android.os.Build
import com.dosparta.trivia.domain.model.AppLanguage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.async
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
@OptIn(ExperimentalCoroutinesApi::class)
class DeepLTextTranslatorTest {

    private val api: DeepLApi = mockk()
    private val apiKey = "test-api-key"

    @Test
    fun `english target returns texts unchanged without calling api`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val texts = listOf("Question 1", "Question 2")

        val result = translator.translate(texts, AppLanguage.ENGLISH)

        assertSame(texts, result)
        coVerify(exactly = 0) { api.translate(any(), any()) }
    }

    @Test
    fun `empty texts returns empty list without calling api`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)

        val result = translator.translate(emptyList(), AppLanguage.GERMAN)

        assertEquals(emptyList<String>(), result)
        coVerify(exactly = 0) { api.translate(any(), any()) }
    }

    @Test
    fun `blank api key throws IllegalStateException`() = runTest {
        val translator = DeepLTextTranslator(api, "")

        var thrown: Throwable? = null
        try {
            translator.translate(listOf("Hello"), AppLanguage.GERMAN)
        } catch (e: Throwable) {
            thrown = e
        }
        assertTrue(thrown is IllegalStateException)
    }

    @Test
    fun `translates texts with correct auth header and language code`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        coEvery {
            api.translate(
                authorization = "DeepL-Auth-Key test-api-key",
                request = DeepLTranslationRequest(
                    text = listOf("Science", "Water"),
                    targetLang = "DE"
                )
            )
        } returns DeepLTranslationResponse(
            translations = listOf(
                DeepLTranslationItem(text = "Wissenschaft"),
                DeepLTranslationItem(text = "Wasser")
            )
        )

        val result = translator.translate(listOf("Science", "Water"), AppLanguage.GERMAN)

        assertEquals(listOf("Wissenschaft", "Wasser"), result)
    }

    @Test
    fun `maps supported languages to DeepL target tags`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)

        coEvery {
            api.translate(any(), match { it.targetLang == "ES" })
        } returns DeepLTranslationResponse(listOf(DeepLTranslationItem(text = "Hola")))

        coEvery {
            api.translate(any(), match { it.targetLang == "ZH" })
        } returns DeepLTranslationResponse(listOf(DeepLTranslationItem(text = "你好")))

        assertEquals(listOf("Hola"), translator.translate(listOf("Hello"), AppLanguage.SPANISH))
        assertEquals(listOf("你好"), translator.translate(listOf("Hello"), AppLanguage.CHINESE_SIMPLIFIED))
    }

    @Test
    fun `memoizes translations and avoids redundant api calls`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        coEvery {
            api.translate(any(), any())
        } returns DeepLTranslationResponse(
            listOf(DeepLTranslationItem(text = "Wasser"))
        )

        val first = translator.translate(listOf("Water"), AppLanguage.GERMAN)
        val second = translator.translate(listOf("Water"), AppLanguage.GERMAN)

        assertEquals(listOf("Wasser"), first)
        assertEquals(listOf("Wasser"), second)
        coVerify(exactly = 1) { api.translate(any(), any()) }
    }

    @Test
    fun `more than fifty texts are batched and cached preserving input order`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val uniqueTexts = (1..101).map { "Text $it" }
        val texts = uniqueTexts + listOf("", "Text 1", "   ", "Text 101")
        val requests = mutableListOf<List<String>>()
        coEvery { api.translate(any(), any()) } coAnswers {
            val request = secondArg<DeepLTranslationRequest>()
            requests += request.text
            DeepLTranslationResponse(request.text.map { DeepLTranslationItem(text = "de:$it") })
        }

        val expected = texts.map { if (it.isBlank()) it else "de:$it" }
        assertEquals(expected, translator.translate(texts, AppLanguage.GERMAN))
        assertEquals(listOf(50, 50, 1), requests.map { it.size })
        assertEquals(uniqueTexts, requests.flatten())

        assertEquals(expected, translator.translate(texts, AppLanguage.GERMAN))
        coVerify(exactly = 3) { api.translate(any(), any()) }
    }

    @Test
    fun `invalid later batch is rejected and only completed batches stay cached`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val texts = (1..51).map { "Text $it" }
        val requests = mutableListOf<List<String>>()
        coEvery { api.translate(any(), any()) } coAnswers {
            val request = secondArg<DeepLTranslationRequest>()
            requests += request.text
            if (requests.size == 2) {
                DeepLTranslationResponse(emptyList())
            } else {
                DeepLTranslationResponse(request.text.map { DeepLTranslationItem(text = "de:$it") })
            }
        }

        var failure: IllegalStateException? = null
        try {
            translator.translate(texts, AppLanguage.GERMAN)
        } catch (e: IllegalStateException) {
            failure = e
        }
        assertEquals("DeepL returned 0 translations for 1 texts.", failure?.message)
        assertEquals(listOf(texts.take(50), texts.takeLast(1)), requests)

        assertEquals(texts.map { "de:$it" }, translator.translate(texts, AppLanguage.GERMAN))
        assertEquals(listOf(texts.take(50), texts.takeLast(1), texts.takeLast(1)), requests)
        coVerify(exactly = 3) { api.translate(any(), any()) }
    }

    @Test
    fun `concurrent overlapping requests translate shared text only once`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val releaseFirstRequest = CompletableDeferred<Unit>()
        val requests = mutableListOf<List<String>>()
        coEvery { api.translate(any(), any()) } coAnswers {
            val request = secondArg<DeepLTranslationRequest>()
            requests += request.text
            if (requests.size == 1) releaseFirstRequest.await()
            DeepLTranslationResponse(request.text.map { DeepLTranslationItem(text = "de:$it") })
        }

        val first = async {
            translator.translate(listOf("Science", "Water"), AppLanguage.GERMAN)
        }
        runCurrent()
        val second = async {
            translator.translate(listOf("Water", "", "Ice", "Water"), AppLanguage.GERMAN)
        }
        runCurrent()

        assertEquals(listOf(listOf("Science", "Water")), requests)
        assertFalse(second.isCompleted)
        releaseFirstRequest.complete(Unit)

        assertEquals(listOf("de:Science", "de:Water"), first.await())
        assertEquals(listOf("de:Water", "", "de:Ice", "de:Water"), second.await())
        assertEquals(listOf(listOf("Science", "Water"), listOf("Ice")), requests)
        coVerify(exactly = 2) { api.translate(any(), any()) }
    }

    @Test
    fun `concurrent identical requests share the cached result`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val releaseRequest = CompletableDeferred<Unit>()
        coEvery { api.translate(any(), any()) } coAnswers {
            releaseRequest.await()
            DeepLTranslationResponse(listOf(DeepLTranslationItem(text = "Wasser")))
        }

        val first = async { translator.translate(listOf("Water"), AppLanguage.GERMAN) }
        runCurrent()
        val second = async { translator.translate(listOf("Water"), AppLanguage.GERMAN) }
        runCurrent()

        coVerify(exactly = 1) { api.translate(any(), any()) }
        releaseRequest.complete(Unit)
        assertEquals(listOf("Wasser"), first.await())
        assertEquals(listOf("Wasser"), second.await())
        coVerify(exactly = 1) { api.translate(any(), any()) }
    }

    @Test
    fun `different languages translate concurrently`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val releaseGermanRequest = CompletableDeferred<Unit>()
        coEvery { api.translate(any(), any()) } coAnswers {
            val request = secondArg<DeepLTranslationRequest>()
            if (request.targetLang == "DE") releaseGermanRequest.await()
            DeepLTranslationResponse(listOf(DeepLTranslationItem(text = "${request.targetLang}:Water")))
        }

        val german = async { translator.translate(listOf("Water"), AppLanguage.GERMAN) }
        runCurrent()
        val spanish = async { translator.translate(listOf("Water"), AppLanguage.SPANISH) }
        runCurrent()

        assertFalse(german.isCompleted)
        assertTrue(spanish.isCompleted)
        assertEquals(listOf("ES:Water"), spanish.await())
        releaseGermanRequest.complete(Unit)
        assertEquals(listOf("DE:Water"), german.await())
    }

    @Test
    fun `cancelled request releases language lock for a waiting caller`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        val releaseRequest = CompletableDeferred<Unit>()
        var requests = 0
        coEvery { api.translate(any(), any()) } coAnswers {
            requests++
            if (requests == 1) releaseRequest.await()
            DeepLTranslationResponse(listOf(DeepLTranslationItem(text = "Wasser")))
        }

        val first = async { translator.translate(listOf("Water"), AppLanguage.GERMAN) }
        runCurrent()
        val second = async { translator.translate(listOf("Water"), AppLanguage.GERMAN) }
        runCurrent()
        assertEquals(1, requests)

        first.cancel()
        first.join()
        assertEquals(listOf("Wasser"), second.await())
        assertEquals(2, requests)
    }

    @Test
    fun `preserves blank texts without passing to api`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        coEvery {
            api.translate(any(), match { it.text == listOf("Hello") })
        } returns DeepLTranslationResponse(listOf(DeepLTranslationItem(text = "Hallo")))

        val result = translator.translate(listOf("", "Hello", "   "), AppLanguage.GERMAN)

        assertEquals(listOf("", "Hallo", "   "), result)
    }

    @Test
    fun `propagates api failure`() = runTest {
        val translator = DeepLTextTranslator(api, apiKey)
        coEvery { api.translate(any(), any()) } throws IOException("Quota exceeded")

        var thrown: Throwable? = null
        try {
            translator.translate(listOf("Hello"), AppLanguage.GERMAN)
        } catch (e: Throwable) {
            thrown = e
        }
        assertTrue(thrown is IOException)
    }
}
