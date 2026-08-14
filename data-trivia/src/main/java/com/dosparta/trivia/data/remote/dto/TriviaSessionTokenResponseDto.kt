package com.dosparta.trivia.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TriviaSessionTokenResponseDto(
    @param:Json(name = "response_code")
    val responseCode: Int,
    @param:Json(name = "response_message")
    val responseMessage: String? = null,
    @param:Json(name = "token")
    val token: String? = null
)
