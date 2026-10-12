package com.dosparta.trivia.data.localization

import android.os.Build
import com.dosparta.trivia.domain.localization.ITextTranslator
import com.dosparta.trivia.domain.model.AppLanguage
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
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
class DualEngineTextTranslatorTest {

    private val primary: ITextTranslator = mockk()
    private val fallback: ITextTranslator = mockk()
    private val dualTranslator = DualEngineTextTranslator(primary, fallback)

    @Test
    fun `english target returns original texts without invoking primary or fallback`() = runTest {
        val texts = listOf("Question 1", "Question 2")

        val result = dualTranslator.translate(texts, AppLanguage.ENGLISH)

        assertSame(texts, result)
        coVerify(exactly = 0) { primary.translate(any(), any()) }
        coVerify(exactly = 0) { fallback.translate(any(), any()) }
    }

    @Test
    fun `empty texts returns empty list without invoking primary or fallback`() = runTest {
        val result = dualTranslator.translate(emptyList(), AppLanguage.GERMAN)

        assertEquals(emptyList<String>(), result)
        coVerify(exactly = 0) { primary.translate(any(), any()) }
        coVerify(exactly = 0) { fallback.translate(any(), any()) }
    }

    @Test
    fun `primary success returns primary translation without invoking fallback`() = runTest {
        val input = listOf("Hello")
        val translated = listOf("Hallo")
        coEvery { primary.translate(input, AppLanguage.GERMAN) } returns translated

        val result = dualTranslator.translate(input, AppLanguage.GERMAN)

        assertEquals(translated, result)
        coVerify(exactly = 1) { primary.translate(input, AppLanguage.GERMAN) }
        coVerify(exactly = 0) { fallback.translate(any(), any()) }
    }

    @Test
    fun `primary failure delegates to fallback translator and succeeds`() = runTest {
        val input = listOf("Hello")
        val fallbackResult = listOf("Hallo (ML Kit)")
        coEvery { primary.translate(input, AppLanguage.GERMAN) } throws IllegalStateException("API key missing")
        coEvery { fallback.translate(input, AppLanguage.GERMAN) } returns fallbackResult

        val result = dualTranslator.translate(input, AppLanguage.GERMAN)

        assertEquals(fallbackResult, result)
        coVerify(exactly = 1) { primary.translate(input, AppLanguage.GERMAN) }
        coVerify(exactly = 1) { fallback.translate(input, AppLanguage.GERMAN) }
    }

    @Test
    fun `cancellation exception from primary is rethrown without invoking fallback`() = runTest {
        val input = listOf("Hello")
        coEvery { primary.translate(input, AppLanguage.GERMAN) } throws CancellationException("Coroutine cancelled")

        var thrown: Throwable? = null
        try {
            dualTranslator.translate(input, AppLanguage.GERMAN)
        } catch (e: Throwable) {
            thrown = e
        }
        assertTrue(thrown is CancellationException)
        coVerify(exactly = 0) { fallback.translate(any(), any()) }
    }

    @Test
    fun `both primary and fallback failure throws exception`() = runTest {
        val input = listOf("Hello")
        coEvery { primary.translate(input, AppLanguage.GERMAN) } throws IOException("DeepL network timeout")
        coEvery { fallback.translate(input, AppLanguage.GERMAN) } throws IOException("ML Kit model download failed")

        var thrown: Throwable? = null
        try {
            dualTranslator.translate(input, AppLanguage.GERMAN)
        } catch (e: Throwable) {
            thrown = e
        }
        assertTrue(thrown is IOException)
        coVerify(exactly = 1) { primary.translate(input, AppLanguage.GERMAN) }
        coVerify(exactly = 1) { fallback.translate(input, AppLanguage.GERMAN) }
    }
}
