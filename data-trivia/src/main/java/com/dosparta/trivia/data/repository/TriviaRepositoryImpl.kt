package com.dosparta.trivia.data.repository

import com.dosparta.core.network.qualifiers.IoDispatcher
import com.dosparta.trivia.data.mapper.TriviaMapper
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.token.TriviaSessionTokenProvider
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class TriviaRepositoryImpl @Inject constructor(
    private val api: TriviaApi,
    private val tokenProvider: TriviaSessionTokenProvider,
    @param:IoDispatcher @field:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ITriviaRepository {
    override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> =
        withContext(ioDispatcher) {
            val response = fetchQuestionsWithTokenRecovery(config)
            when (response.responseCode) {
                RESPONSE_CODE_SUCCESS -> {
                    tokenProvider.markTokenUsed()
                    TriviaMapper.fromResponse(response)
                }
                RESPONSE_CODE_NO_RESULTS -> throw IllegalStateException(
                    "Trivia API returned no results for the selected filters."
                )
                RESPONSE_CODE_INVALID_PARAMETER -> throw IllegalStateException(
                    "Trivia API rejected query parameters."
                )
                RESPONSE_CODE_RATE_LIMIT -> throw IllegalStateException(
                    "Trivia API rate limit reached. Please wait a few seconds and retry."
                )
                else -> throw IllegalStateException(
                    "Trivia API returned response_code=${response.responseCode}."
                )
            }
        }

    override suspend fun getCategories(): List<TriviaCategory> =
        withContext(ioDispatcher) {
            val dto = api.fetchCategories()
            TriviaMapper.fromCategoriesResponse(dto)
        }

    private suspend fun fetchQuestionsWithTokenRecovery(config: TriviaConfig) =
        run {
            var token = tokenProvider.getValidToken()
            var response = fetchQuestions(config, token)

            if (response.responseCode == RESPONSE_CODE_TOKEN_NOT_FOUND) {
                tokenProvider.clearToken()
                token = tokenProvider.getValidToken()
                response = fetchQuestions(config, token)
            } else if (response.responseCode == RESPONSE_CODE_TOKEN_EMPTY) {
                token = tokenProvider.resetToken(token)
                response = fetchQuestions(config, token)
            }

            response
        }

    private suspend fun fetchQuestions(config: TriviaConfig, token: String) =
        api.fetchQuestions(
            amount = config.amount,
            category = config.categoryId,
            difficulty = config.normalizedDifficulty(),
            token = token
        )

    private companion object {
        const val RESPONSE_CODE_SUCCESS = 0
        const val RESPONSE_CODE_NO_RESULTS = 1
        const val RESPONSE_CODE_INVALID_PARAMETER = 2
        const val RESPONSE_CODE_TOKEN_NOT_FOUND = 3
        const val RESPONSE_CODE_TOKEN_EMPTY = 4
        const val RESPONSE_CODE_RATE_LIMIT = 5
    }
}
