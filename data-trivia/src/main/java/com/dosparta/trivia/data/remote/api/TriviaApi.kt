package com.dosparta.trivia.data.remote.api

import com.dosparta.trivia.data.remote.dto.TriviaCategoriesResponseDto
import com.dosparta.trivia.data.remote.dto.TriviaResponseDto
import com.dosparta.trivia.data.remote.dto.TriviaSessionTokenResponseDto
import retrofit2.http.GET
import retrofit2.http.Query

interface TriviaApi {
    /**
     * Fetches the list of available categories from OpenTDB.
     */
    @GET("api_category.php")
    suspend fun fetchCategories(): TriviaCategoriesResponseDto

    /**
     * Fetches trivia questions with optional filters.
     *
     * @param amount number of questions to fetch, between 10 and 50
     * @param category optional category id, e.g. 9 for General Knowledge
     * @param difficulty optional difficulty filter: easy, medium, hard
     * @return the raw response DTO, whose `results` you’ll map downstream
     */
    @GET("api.php")
    suspend fun fetchQuestions(
        @Query("amount") amount: Int,
        @Query("category") category: Int? = null,
        @Query("difficulty") difficulty: String? = null,
        @Query("token") token: String? = null
    ): TriviaResponseDto

    @GET("api_token.php")
    suspend fun requestSessionToken(
        @Query("command") command: String = "request"
    ): TriviaSessionTokenResponseDto

    @GET("api_token.php")
    suspend fun resetSessionToken(
        @Query("command") command: String = "reset",
        @Query("token") token: String
    ): TriviaSessionTokenResponseDto
}
