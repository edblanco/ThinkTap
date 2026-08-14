package com.dosparta.trivia.data.remote.dto

import com.squareup.moshi.Json
import com.squareup.moshi.JsonClass

@JsonClass(generateAdapter = true)
data class TriviaQuestionDto(
    @param:Json(name = "type")
    val type: String,

    @param:Json(name = "difficulty")
    val difficulty: String,

    @param:Json(name = "category")
    val category: String,

    @param:Json(name = "question")
    val question: String,

    @param:Json(name = "correct_answer")
    val correctAnswer: String,

    @param:Json(name = "incorrect_answers")
    val incorrectAnswers: List<String>
)

