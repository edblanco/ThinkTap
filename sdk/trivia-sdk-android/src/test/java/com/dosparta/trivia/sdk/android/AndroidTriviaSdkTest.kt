package com.dosparta.trivia.sdk.android

import android.content.Context
import android.os.Build
import com.dosparta.trivia.domain.model.AppLanguage
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import com.dosparta.trivia.sdk.TriviaState
import io.mockk.every
import io.mockk.mockk
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotSame
import org.junit.Assert.assertSame
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class AndroidTriviaSdkTest {
    @Test
    fun `missing application context is an invalid configuration`() {
        val callerContext = mockk<Context>()
        every { callerContext.applicationContext } returns null

        val error = assertThrows(TriviaSdkException::class.java) {
            AndroidTriviaSdk.create(callerContext)
        }
        assertEquals(TriviaError.INVALID_CONFIGURATION, error.error)
    }

    @Test
    fun `create uses application context and returns independent controllers without Hilt`() {
        val callerContext = mockk<Context>()
        every { callerContext.applicationContext } returns RuntimeEnvironment.getApplication()

        val first = AndroidTriviaSdk.create(callerContext)
        val second = AndroidTriviaSdk.create(callerContext)

        assertNotSame(first, second)
        assertNotSame(first.state, second.state)
        assertEquals(TriviaState.Idle, first.state.value)
        assertEquals(TriviaState.Idle, second.state.value)
        assertSame(first.translationUnavailable, second.translationUnavailable)
        // The caller context is strict: touching its storage APIs would fail the test.
        first.setContentLanguage(AppLanguage.ENGLISH)
        assertTrue(first.setContentLanguage(AppLanguage.GERMAN))
        assertFalse(second.setContentLanguage(AppLanguage.GERMAN))
        assertTrue(second.setContentLanguage(AppLanguage.ENGLISH))
        assertFalse(first.setContentLanguage(AppLanguage.ENGLISH))
    }

    @Test
    fun `createEnglishOnly missing application context is an invalid configuration`() {
        val callerContext = mockk<Context>()
        every { callerContext.applicationContext } returns null

        val error = assertThrows(TriviaSdkException::class.java) {
            AndroidTriviaSdk.createEnglishOnly(callerContext)
        }
        assertEquals(TriviaError.INVALID_CONFIGURATION, error.error)
    }

    @Test
    fun `createEnglishOnly uses application context and enforces english-only language support`() {
        val callerContext = mockk<Context>()
        every { callerContext.applicationContext } returns RuntimeEnvironment.getApplication()

        val first = AndroidTriviaSdk.createEnglishOnly(callerContext)
        val second = AndroidTriviaSdk.createEnglishOnly(callerContext)

        assertNotSame(first, second)
        assertNotSame(first.state, second.state)
        assertEquals(TriviaState.Idle, first.state.value)
        assertEquals(TriviaState.Idle, second.state.value)
        assertSame(first.translationUnavailable, second.translationUnavailable)
        assertEquals(AppLanguage.ENGLISH, first.contentLanguage.value)
        assertFalse(first.setContentLanguage(AppLanguage.GERMAN))
        assertEquals(AppLanguage.ENGLISH, first.contentLanguage.value)
        assertEquals(AppLanguage.ENGLISH, second.contentLanguage.value)
        assertFalse(second.setContentLanguage(AppLanguage.ENGLISH))
    }
}
