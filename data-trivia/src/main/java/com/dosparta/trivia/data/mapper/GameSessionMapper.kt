package com.dosparta.trivia.data.mapper

import com.dosparta.trivia.data.local.entity.GameSessionEntity
import com.dosparta.trivia.domain.model.TriviaQuestion
import com.dosparta.trivia.domain.repository.GameSessionState
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken

object GameSessionMapper {

    private val gson = Gson()

    fun toEntity(
        questions: List<TriviaQuestion>,
        currentIndex: Int,
        correctCount: Int,
        activeElapsedMillis: Long,
        selectedAnswers: Map<Int, String>
    ): GameSessionEntity {
        return GameSessionEntity(
            sessionId = GameSessionEntity.ACTIVE_SESSION_ID,
            amount = questions.size,
            categoryId = null,
            difficulty = null,
            currentIndex = currentIndex,
            correctCount = correctCount,
            startedAtMillis = activeElapsedMillis,
            questionsJson = gson.toJson(questions),
            selectedAnswersJson = gson.toJson(selectedAnswers),
            status = GameSessionEntity.STATUS_IN_PROGRESS
        )
    }

    fun fromEntity(entity: GameSessionEntity): GameSessionState {
        val questionsType = object : TypeToken<List<TriviaQuestion>>() {}.type
        val questions = gson.fromJson<List<TriviaQuestion>>(entity.questionsJson, questionsType)
            ?: emptyList()

        val answersType = object : TypeToken<Map<Int, String>>() {}.type
        val selectedAnswers = gson.fromJson<Map<Int, String>>(entity.selectedAnswersJson ?: "{}", answersType)
            ?: emptyMap()

        return GameSessionState(
            questions = questions,
            currentIndex = entity.currentIndex,
            correctCount = entity.correctCount,
            activeElapsedMillis = entity.startedAtMillis,
            selectedAnswers = selectedAnswers
        )
    }
}
