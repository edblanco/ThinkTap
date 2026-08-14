package com.dosparta.trivia.data.token

import android.os.Build
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.dto.TriviaSessionTokenResponseDto
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.mockito.Mock
import org.mockito.MockitoAnnotations
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaSessionTokenProviderImplTest {

    @Mock
    private lateinit var api: TriviaApi

    private lateinit var store: SessionTokenStore
    private lateinit var provider: TriviaSessionTokenProviderImpl

    @Before
    fun setUp() {
        MockitoAnnotations.openMocks(this)
        val context = RuntimeEnvironment.getApplication().applicationContext
        store = SessionTokenStore(context)
        store.clear()
        provider = TriviaSessionTokenProviderImpl(api, store)
    }

    @Test
    fun `getValidToken requests new token when none is stored`() {
        runBlocking {
            whenever(api.requestSessionToken()).thenReturn(
                TriviaSessionTokenResponseDto(responseCode = 0, token = "fresh-token")
            )

            val token = provider.getValidToken()

            assertEquals("fresh-token", token)
            assertEquals("fresh-token", store.getToken())
        }
    }

    @Test
    fun `getValidToken reuses stored token when not expired`() {
        runBlocking {
            val now = System.currentTimeMillis()
            store.saveToken("cached-token", now)

            val token = provider.getValidToken()

            assertEquals("cached-token", token)
        }
    }

    @Test
    fun `getValidToken refreshes token after inactivity window`() {
        runBlocking {
            val expired = System.currentTimeMillis() - (6 * 60 * 60 * 1000L) - 1L
            store.saveToken("old-token", expired)
            whenever(api.requestSessionToken()).thenReturn(
                TriviaSessionTokenResponseDto(responseCode = 0, token = "new-token")
            )

            val token = provider.getValidToken()

            assertEquals("new-token", token)
            verify(api).requestSessionToken()
        }
    }

    @Test
    fun `resetToken keeps existing token when API omits token field`() {
        runBlocking {
            whenever(api.resetSessionToken(command = "reset", token = "token-a")).thenReturn(
                TriviaSessionTokenResponseDto(responseCode = 0, token = null)
            )

            val token = provider.resetToken("token-a")

            assertEquals("token-a", token)
            assertEquals("token-a", store.getToken())
        }
    }

    @Test
    fun `request token failure throws`() {
        runBlocking {
            whenever(api.requestSessionToken()).thenReturn(
                TriviaSessionTokenResponseDto(responseCode = 5, token = null)
            )

            try {
                provider.getValidToken()
                fail("Expected exception not thrown")
            } catch (e: IllegalStateException) {
                assertTrue(e.message.orEmpty().contains("token request failed", ignoreCase = true))
            }
        }
    }
}
