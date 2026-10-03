package com.dosparta.trivia.data.repository

import com.dosparta.core.network.qualifiers.IoDispatcher
import com.dosparta.trivia.data.mapper.TriviaMapper
import com.dosparta.trivia.data.remote.api.TriviaApi
import com.dosparta.trivia.data.remote.networkCall
import com.dosparta.trivia.data.token.TriviaSessionTokenProvider
import com.dosparta.trivia.domain.model.TriviaCategory
import com.dosparta.trivia.domain.model.TriviaConfig
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.ITriviaRepository
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class TriviaRepositoryImpl @Inject constructor(
    private val api: TriviaApi,
    private val tokenProvider: TriviaSessionTokenProvider,
    @param:IoDispatcher @field:IoDispatcher private val ioDispatcher: CoroutineDispatcher
) : ITriviaRepository {
    private val questionsMutex = Mutex()

    override suspend fun getQuestions(config: TriviaConfig): List<TriviaQuestion> =
        withContext(ioDispatcher) {
            // Serialize token recovery across controllers sharing the same token store.
            questionsMutex.withLock {
                networkCall {
                    val response = fetchQuestionsWithTokenRecovery(config)
                    TriviaMapper.fromResponse(response).also {
                        tokenProvider.markTokenUsed()
                    }
                }
            }
        }

    override suspend fun getCategories(): List<TriviaCategory> =
        withContext(ioDispatcher) {
            networkCall {
                TriviaMapper.fromCategoriesResponse(api.fetchCategories())
            }
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
        const val RESPONSE_CODE_TOKEN_NOT_FOUND = 3
        const val RESPONSE_CODE_TOKEN_EMPTY = 4
    }
}
