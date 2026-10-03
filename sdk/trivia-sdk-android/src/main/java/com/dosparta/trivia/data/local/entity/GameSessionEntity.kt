package com.dosparta.trivia.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.PrimaryKey

@Entity(tableName = "game_sessions")
internal data class GameSessionEntity(
    @PrimaryKey
    val sessionId: String = ACTIVE_SESSION_ID,
    val amount: Int,
    @ColumnInfo(name = "category_id")
    val categoryId: Int?,
    val difficulty: String?,
    @ColumnInfo(name = "current_index")
    val currentIndex: Int = 0,
    @ColumnInfo(name = "correct_count")
    val correctCount: Int = 0,
    @ColumnInfo(name = "started_at_millis")
    val startedAtMillis: Long = System.currentTimeMillis(),
    @ColumnInfo(name = "questions_json")
    val questionsJson: String,
    @ColumnInfo(name = "selected_answers_json")
    val selectedAnswersJson: String? = null,
    @ColumnInfo(name = "status")
    val status: String = STATUS_IN_PROGRESS
) {
    companion object {
        const val ACTIVE_SESSION_ID = "active_game_session"
        const val STATUS_IN_PROGRESS = "in_progress"
        const val STATUS_FINISHED = "finished"
    }
}
