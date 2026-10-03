package com.dosparta.trivia.data.remote

import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import com.squareup.moshi.JsonDataException
import com.squareup.moshi.JsonEncodingException
import kotlinx.coroutines.CancellationException
import retrofit2.HttpException
import java.io.IOException

private const val RESPONSE_NO_RESULTS = 1
private const val RESPONSE_INVALID_QUERY = 2
private const val RESPONSE_RATE_LIMIT = 5
private const val HTTP_SERVER_ERROR_MIN = 500
private const val HTTP_REQUEST_TIMEOUT = 408
private const val HTTP_TOO_MANY_REQUESTS = 429

internal fun responseCodeError(code: Int): TriviaSdkException = TriviaSdkException(
    when (code) {
        RESPONSE_NO_RESULTS -> TriviaError.NO_RESULTS
        RESPONSE_INVALID_QUERY -> TriviaError.INVALID_QUERY
        RESPONSE_RATE_LIMIT -> TriviaError.RATE_LIMIT
        else -> TriviaError.PROTOCOL
    }
)

internal suspend fun <T> networkCall(block: suspend () -> T): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: TriviaSdkException) {
    throw error
} catch (error: HttpException) {
    throw TriviaSdkException(
        if (error.code() >= HTTP_SERVER_ERROR_MIN ||
            error.code() == HTTP_REQUEST_TIMEOUT ||
            error.code() == HTTP_TOO_MANY_REQUESTS
        ) {
            TriviaError.NETWORK
        } else {
            TriviaError.PROTOCOL
        },
        error
    )
} catch (error: JsonDataException) {
    throw TriviaSdkException(TriviaError.PROTOCOL, error)
} catch (error: JsonEncodingException) {
    throw TriviaSdkException(TriviaError.PROTOCOL, error)
} catch (error: IOException) {
    throw TriviaSdkException(TriviaError.NETWORK, error)
} catch (error: Exception) {
    throw TriviaSdkException(TriviaError.UNKNOWN, error)
}

internal inline fun <T> persistenceCall(block: () -> T): T = try {
    block()
} catch (error: CancellationException) {
    throw error
} catch (error: TriviaSdkException) {
    throw error
} catch (error: Exception) {
    throw TriviaSdkException(TriviaError.PERSISTENCE, error)
}
