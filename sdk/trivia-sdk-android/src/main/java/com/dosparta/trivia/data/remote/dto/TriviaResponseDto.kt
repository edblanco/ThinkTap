package com.dosparta.trivia.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = false)
internal data class TriviaResponseDto(
    @param:Json(name = "response_code")
    val responseCode: Int,

    @param:Json(name = "results")
    val results: List<TriviaQuestionDto>
)
