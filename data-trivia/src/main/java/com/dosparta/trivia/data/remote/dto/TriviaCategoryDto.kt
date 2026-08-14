package com.dosparta.trivia.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TriviaCategoryDto(
    @param:Json(name = "id")
    val id: Int,

    @param:Json(name = "name")
    val name: String
)

@JsonClass(generateAdapter = true)
data class TriviaCategoriesResponseDto(
    @param:Json(name = "trivia_categories")
    val triviaCategories: List<TriviaCategoryDto>
)
