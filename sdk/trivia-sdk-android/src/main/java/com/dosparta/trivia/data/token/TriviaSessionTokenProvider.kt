package com.dosparta.trivia.data.token

import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.networkCall
import com.dosparta.trivia.data.remote.persistenceCall
import com.dosparta.trivia.data.remote.responseCodeError
import com.dosparta.trivia.sdk.TriviaError
import com.dosparta.trivia.sdk.TriviaSdkException
import javax.inject.Inject
import javax.inject.Singleton

internal interface TriviaSessionTokenProvider {
    suspend fun getValidToken(): String
    suspend fun resetToken(existingToken: String): String
    fun markTokenUsed()
    fun clearToken()
}

@Singleton
internal class TriviaSessionTokenProviderImpl @Inject constructor(
    private val api: TriviaApi,
    private val tokenStore: SessionTokenStore
) : TriviaSessionTokenProvider {

    override suspend fun getValidToken(): String {
        val now = System.currentTimeMillis()
        val token = persistenceCall { tokenStore.getToken() }
        val lastUsedAt = persistenceCall { tokenStore.getLastUsedAtMillis() }
        val isExpired = now - lastUsedAt > TOKEN_TTL_MILLIS
        if (token.isNullOrBlank() || lastUsedAt == 0L || isExpired) {
            return requestNewToken(now)
        }
        return token
    }

    override suspend fun resetToken(existingToken: String): String {
        val response = networkCall { api.resetSessionToken(token = existingToken) }
        if (response.responseCode != RESPONSE_CODE_SUCCESS) {
            throw responseCodeError(response.responseCode)
        }

        val token = response.token?.takeIf { it.isNotBlank() } ?: existingToken
        persistenceCall { tokenStore.saveToken(token, System.currentTimeMillis()) }
        return token
    }

    override fun markTokenUsed() {
        persistenceCall { tokenStore.updateLastUsedAt(System.currentTimeMillis()) }
    }

    override fun clearToken() {
        persistenceCall { tokenStore.clear() }
    }

    private suspend fun requestNewToken(now: Long): String {
        val response = networkCall { api.requestSessionToken() }
        if (response.responseCode != RESPONSE_CODE_SUCCESS) {
            throw responseCodeError(response.responseCode)
        }

        val token = response.token?.takeIf { it.isNotBlank() }
            ?: throw TriviaSdkException(TriviaError.PROTOCOL)
        persistenceCall { tokenStore.saveToken(token, now) }
        return token
    }

    private companion object {
        const val RESPONSE_CODE_SUCCESS = 0
        const val TOKEN_TTL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
