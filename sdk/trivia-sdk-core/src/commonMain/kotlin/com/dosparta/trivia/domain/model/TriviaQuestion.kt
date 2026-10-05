package com.dosparta.trivia.domain.model

/**
 * A trivia question with all the pieces your UI and business logic need.
 */

data class TriviaQuestion(
    val category: String,
    val type: String,          // e.g. "multiple" or "boolean"
    val difficulty: String,    // e.g. "easy", "medium", "hard"
    val question: String,      // already HTML-decoded
    val correctAnswer: String,
    val options: List<String>  // shuffled list of all possible answers
)
