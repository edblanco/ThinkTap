package com.dosparta.trivia.data.token

import com.dosparta.trivia.data.remote.api.TriviaApi
import javax.inject.Inject
import javax.inject.Singleton

interface TriviaSessionTokenProvider {
    suspend fun getValidToken(): String
    suspend fun resetToken(existingToken: String): String
    fun markTokenUsed()
    fun clearToken()
}

@Singleton
class TriviaSessionTokenProviderImpl @Inject constructor(
    private val api: TriviaApi,
    private val tokenStore: SessionTokenStore
) : TriviaSessionTokenProvider {

    override suspend fun getValidToken(): String {
        val now = System.currentTimeMillis()
        val token = tokenStore.getToken()
        val lastUsedAt = tokenStore.getLastUsedAtMillis()
        val isExpired = now - lastUsedAt > TOKEN_TTL_MILLIS
        if (token.isNullOrBlank() || lastUsedAt == 0L || isExpired) {
            return requestNewToken(now)
        }
        return token
    }

    override suspend fun resetToken(existingToken: String): String {
        val response = api.resetSessionToken(token = existingToken)
        if (response.responseCode != RESPONSE_CODE_SUCCESS) {
            throw IllegalStateException("Trivia API token reset failed: response_code=${response.responseCode}.")
        }

        val token = response.token?.takeIf { it.isNotBlank() } ?: existingToken
        tokenStore.saveToken(token, System.currentTimeMillis())
        return token
    }

    override fun markTokenUsed() {
        tokenStore.updateLastUsedAt(System.currentTimeMillis())
    }

    override fun clearToken() {
        tokenStore.clear()
    }

    private suspend fun requestNewToken(now: Long): String {
        val response = api.requestSessionToken()
        if (response.responseCode != RESPONSE_CODE_SUCCESS) {
            throw IllegalStateException("Trivia API token request failed: response_code=${response.responseCode}.")
        }

        val token = response.token?.takeIf { it.isNotBlank() }
            ?: throw IllegalStateException("Trivia API returned an empty session token.")
        tokenStore.saveToken(token, now)
        return token
    }

    private companion object {
        const val RESPONSE_CODE_SUCCESS = 0
        const val TOKEN_TTL_MILLIS = 6 * 60 * 60 * 1000L
    }
}
