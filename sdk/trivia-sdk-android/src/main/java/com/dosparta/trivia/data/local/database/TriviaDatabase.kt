package com.dosparta.trivia.data.local.database

import androidx.room.Database
import androidx.room.RoomDatabase
import com.dosparta.trivia.data.local.dao.GameSessionDao
import com.dosparta.trivia.data.local.entity.GameSessionEntity

@Database(
    entities = [GameSessionEntity::class],
    version = 1,
    exportSchema = false
)
internal abstract class TriviaDatabase : RoomDatabase() {
    abstract fun gameSessionDao(): GameSessionDao
}
