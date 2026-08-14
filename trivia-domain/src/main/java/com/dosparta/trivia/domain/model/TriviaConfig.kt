package com.dosparta.trivia.domain.model

/**
 * User-selected game configuration for a trivia session.
 *
 * The config keeps the request parameters explicit and easy to validate before
 * we start loading questions from the network.
 */
data class TriviaConfig(
    val amount: Int = DEFAULT_AMOUNT,
    val categoryId: Int? = null,
    val difficulty: String? = null
) {
    init {
        require(amount in MIN_AMOUNT..MAX_AMOUNT) {
            "Amount must be between $MIN_AMOUNT and $MAX_AMOUNT."
        }

        difficulty?.let { selectedDifficulty ->
            val normalized = selectedDifficulty.trim().lowercase()
            require(normalized in VALID_DIFFICULTIES) {
                "Difficulty must be one of ${VALID_DIFFICULTIES.joinToString()}."
            }
        }
    }

    fun normalizedDifficulty(): String? {
        val normalized = difficulty?.trim()?.lowercase() ?: return null
        return when (normalized) {
            MIXED_DIFFICULTY -> null
            in VALID_DIFFICULTIES -> normalized
            else -> null
        }
    }

    companion object {
        const val MIN_AMOUNT = 10
        const val MAX_AMOUNT = 50
        const val DEFAULT_AMOUNT = 10
        const val MIXED_DIFFICULTY = "mixed"
        val VALID_DIFFICULTIES = setOf("easy", "medium", "hard", MIXED_DIFFICULTY)
    }
}
