package com.dosparta.trivia.data.token

import android.os.Build
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.dto.TriviaSessionTokenResponseDto
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import kotlinx.coroutines.runBlocking
import io.mockk.MockKAnnotations
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.impl.annotations.MockK
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.io.IOException

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaSessionTokenProviderImplTest {

    @MockK
    private lateinit var api: TriviaApi

    private lateinit var store: SessionTokenStore
    private lateinit var provider: TriviaSessionTokenProviderImpl

    @Before
    fun setUp() {
        MockKAnnotations.init(this)
        val context = RuntimeEnvironment.getApplication().applicationContext
        store = SessionTokenStore(context)
        store.clear()
        provider = TriviaSessionTokenProviderImpl(api, store)
    }

    @Test
    fun `getValidToken requests new token when none is stored`() {
        runBlocking {
            coEvery { api.requestSessionToken() } returns
                TriviaSessionTokenResponseDto(responseCode = 0, token = "fresh-token")

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
            coEvery { api.requestSessionToken() } returns
                TriviaSessionTokenResponseDto(responseCode = 0, token = "new-token")

            val token = provider.getValidToken()

            assertEquals("new-token", token)
            coVerify { api.requestSessionToken() }
        }
    }

    @Test
    fun `resetToken keeps existing token when API omits token field`() {
        runBlocking {
            coEvery { api.resetSessionToken(command = "reset", token = "token-a") } returns
                TriviaSessionTokenResponseDto(responseCode = 0, token = null)

            val token = provider.resetToken("token-a")

            assertEquals("token-a", token)
            assertEquals("token-a", store.getToken())
        }
    }

    @Test
    fun `request token failure throws`() {
        runBlocking {
            coEvery { api.requestSessionToken() } returns
                TriviaSessionTokenResponseDto(responseCode = 5, token = null)

            try {
                provider.getValidToken()
                fail("Expected exception not thrown")
            } catch (e: TriviaSdkException) {
                assertEquals(TriviaError.RATE_LIMIT, e.error)
            }
        }

    }

    @Test
    fun `empty successful token response is protocol failure and is not stored`() = runBlocking {
        coEvery { api.requestSessionToken() } returns
            TriviaSessionTokenResponseDto(responseCode = 0, token = " ")

        try {
            provider.getValidToken()
            fail("Expected protocol failure")
        } catch (error: TriviaSdkException) {
            assertEquals(TriviaError.PROTOCOL, error.error)
            assertNull(store.getToken())
        }
    }

    @Test
    fun `token request IO failure is typed and retains cause`() = runBlocking {
        val failure = IOException("offline")
        coEvery { api.requestSessionToken() } throws failure

        try {
            provider.getValidToken()
            fail("Expected network failure")
        } catch (error: TriviaSdkException) {
            assertEquals(TriviaError.NETWORK, error.error)
            assertSame(failure, error.cause)
        }
    }

    @Test
    fun `failed token reset preserves existing stored token`() = runBlocking {
        store.saveToken("existing-token", System.currentTimeMillis())
        coEvery { api.resetSessionToken(command = "reset", token = "existing-token") } returns
            TriviaSessionTokenResponseDto(responseCode = 3, token = null)

        try {
            provider.resetToken("existing-token")
            fail("Expected protocol failure")
        } catch (error: TriviaSdkException) {
            assertEquals(TriviaError.PROTOCOL, error.error)
            assertEquals("existing-token", store.getToken())
        }
    }
}
