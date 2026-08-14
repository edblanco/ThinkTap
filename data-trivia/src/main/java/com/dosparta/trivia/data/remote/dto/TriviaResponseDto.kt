package com.dosparta.trivia.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TriviaResponseDto(
    @param:Json(name = "response_code")
    val responseCode: Int,

    @param:Json(name = "results")
    val results: List<TriviaQuestionDto>
)

