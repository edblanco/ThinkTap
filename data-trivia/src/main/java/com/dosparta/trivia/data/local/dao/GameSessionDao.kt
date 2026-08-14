package com.dosparta.trivia.data.local.dao

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import androidx.room.Update
import com.dosparta.trivia.data.local.entity.GameSessionEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface GameSessionDao {

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrReplaceSession(session: GameSessionEntity)

    @Query("SELECT * FROM game_sessions WHERE sessionId = :sessionId")
    fun getSessionById(sessionId: String): Flow<GameSessionEntity?>

    @Query("SELECT * FROM game_sessions WHERE sessionId = :sessionId")
    suspend fun getSessionByIdOnce(sessionId: String): GameSessionEntity?

    @Query("SELECT * FROM game_sessions WHERE status = 'in_progress' LIMIT 1")
    fun getActiveSession(): Flow<GameSessionEntity?>

    @Query("SELECT * FROM game_sessions WHERE status = 'in_progress' LIMIT 1")
    suspend fun getActiveSessionOnce(): GameSessionEntity?

    @Update
    suspend fun updateSession(session: GameSessionEntity)

    @Delete
    suspend fun deleteSession(session: GameSessionEntity)

    @Query("DELETE FROM game_sessions WHERE sessionId = :sessionId")
    suspend fun deleteSessionById(sessionId: String)

    @Query("DELETE FROM game_sessions WHERE status = 'in_progress'")
    suspend fun clearActiveSession()
}
