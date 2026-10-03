package com.dosparta.trivia.data.remote

import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test
import retrofit2.HttpException
import java.io.IOException

class AdapterErrorsTest {
    @Test
    fun `response codes have stable typed errors`() {
        mapOf(
            1 to TriviaError.NO_RESULTS,
            2 to TriviaError.INVALID_QUERY,
            3 to TriviaError.PROTOCOL,
            4 to TriviaError.PROTOCOL,
            5 to TriviaError.RATE_LIMIT,
            99 to TriviaError.PROTOCOL
        ).forEach { (code, expected) ->
            assertEquals(expected, responseCodeError(code).error)
        }
    }

    @Test
    fun `HTTP failures distinguish connectivity from protocol errors`() = runTest {
        mapOf(
            400 to TriviaError.PROTOCOL,
            404 to TriviaError.PROTOCOL,
            408 to TriviaError.NETWORK,
            429 to TriviaError.NETWORK,
            503 to TriviaError.NETWORK
        ).forEach { (code, expected) ->
            val failure = mockk<HttpException>()
            every { failure.code() } returns code
            assertNetworkFailure(failure, expected)
        }
    }

    @Test
    fun `invalid JSON is protocol failure rather than network failure`() = runTest {
        assertNetworkFailure(JsonDataException("invalid field"), TriviaError.PROTOCOL)
        assertNetworkFailure(JsonEncodingException("invalid JSON"), TriviaError.PROTOCOL)
    }

    @Test
    fun `IO failure retains cause`() = runTest {
        assertNetworkFailure(IOException("offline"), TriviaError.NETWORK)
    }

    @Test
    fun `typed failure is preserved`() = runTest {
        val failure = TriviaSdkException(TriviaError.INVALID_QUERY)
        try {
            networkCall<Unit> { throw failure }
            fail("Expected typed failure")
        } catch (error: TriviaSdkException) {
            assertSame(failure, error)
        }
    }

    @Test
    fun `cancellation is never wrapped`() = runTest {
        val cancellation = CancellationException("cancelled")
        try {
            networkCall<Unit> { throw cancellation }
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
        try {
            persistenceCall<Unit> { throw cancellation }
            fail("Expected cancellation")
        } catch (error: CancellationException) {
            assertSame(cancellation, error)
        }
    }

    @Test
    fun `persistence failure retains cause`() {
        val failure = IllegalStateException("database unavailable")
        try {
            persistenceCall<Unit> { throw failure }
            fail("Expected persistence failure")
        } catch (error: TriviaSdkException) {
            assertEquals(TriviaError.PERSISTENCE, error.error)
            assertSame(failure, error.cause)
        }
    }

    private suspend fun assertNetworkFailure(failure: Exception, expected: TriviaError) {
        try {
            networkCall<Unit> { throw failure }
            fail("Expected network adapter failure")
        } catch (error: TriviaSdkException) {
            assertEquals(expected, error.error)
            assertSame(failure, error.cause)
        }
    }
}
