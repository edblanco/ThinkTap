package com.dosparta.trivia.data.localization.deepl

import android.os.Build
import com.dosparta.trivia.domain.model.AppLanguage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
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
