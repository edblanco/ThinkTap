package com.dosparta.trivia.data.local

import android.content.Context
import android.os.Build
import androidx.room.Room
import com.dosparta.trivia.data.local.database.TriviaDatabase
import com.dosparta.trivia.data.localization.ContentLocalizationRepositoryImpl
import com.dosparta.trivia.data.repository.GameSessionRepositoryImpl
import com.dosparta.trivia.domain.repository.ITriviaRepository
import com.dosparta.trivia.sdk.TriviaSdk
import com.dosparta.trivia.sdk.TriviaState
import io.mockk.mockk
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [Build.VERSION_CODES.N])
class TriviaDatabaseCompatibilityTest {
    @Test
    fun `version one SQLite session survives SDK Room opening and controller restore`() = runBlocking {
        val context = RuntimeEnvironment.getApplication().applicationContext
        context.deleteDatabase(DATABASE_NAME)
        // Build the pre-extraction schema directly, not through the current Room entity.
        // With no Room identity table, Room must validate every legacy column on opening.
        context.openOrCreateDatabase(DATABASE_NAME, Context.MODE_PRIVATE, null).use { legacy ->
            legacy.execSQL(
                """
                CREATE TABLE IF NOT EXISTS `game_sessions` (
                    `sessionId` TEXT NOT NULL,
                    `amount` INTEGER NOT NULL,
                    `category_id` INTEGER,
                    `difficulty` TEXT,
                    `current_index` INTEGER NOT NULL,
                    `correct_count` INTEGER NOT NULL,
                    `started_at_millis` INTEGER NOT NULL,
                    `questions_json` TEXT NOT NULL,
                    `selected_answers_json` TEXT,
                    `status` TEXT NOT NULL,
                    PRIMARY KEY(`sessionId`)
                )
                """.trimIndent()
            )
            legacy.execSQL(
                """
                INSERT INTO game_sessions (
                    sessionId, amount, category_id, difficulty, current_index, correct_count,
                    started_at_millis, questions_json, selected_answers_json, status
                ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                """.trimIndent(),
                arrayOf<Any?>(
                    "active_game_session", 2, null, null, 1, 1, 1250L,
                    LEGACY_QUESTIONS, """{"0":"Water"}""", "in_progress"
                )
            )
            legacy.version = 1
        }

        // Same database name, Room builder and storage adapter used by AndroidTriviaSdk.
        val database = Room.databaseBuilder(
            context, TriviaDatabase::class.java, DATABASE_NAME
        ).build()
        try {
            val storage = GameSessionRepositoryImpl(database.gameSessionDao())
            val saved = storage.getActiveSession()
            assertNotNull(saved)
            requireNotNull(saved)
            assertEquals(2, saved.questions.size)
            assertEquals("Water", saved.questions[0].correctAnswer)
            assertEquals(listOf("Water", "Salt", "Sugar", "Ice"), saved.questions[0].options)
            assertEquals(mapOf(0 to "Water"), saved.selectedAnswers)
            assertEquals(1250L, saved.activeElapsedMillis)
            assertEquals(1, saved.currentIndex)
            assertEquals(1, saved.correctCount)

            val sdk = TriviaSdk(
                repository = mockk<ITriviaRepository>(),
                sessions = storage,
                localization = ContentLocalizationRepositoryImpl()
            )
            assertTrue(sdk.restore())
            val playing = sdk.state.value as TriviaState.Playing
            assertEquals(saved.questions, playing.session.questions)
            assertEquals(1, playing.session.currentIndex)
            assertEquals(1, playing.session.correctCount)
            assertEquals("Was Rome founded in 753 BC?", playing.session.currentQuestion?.question)
            assertEquals(saved, storage.getActiveSession())
        } finally {
            database.close()
            context.deleteDatabase(DATABASE_NAME)
        }
    }

    private companion object {
        const val DATABASE_NAME = "trivia_database"
        val LEGACY_QUESTIONS = """
            [
              {
                "category":"Science",
                "type":"multiple",
                "difficulty":"easy",
                "question":"What is H2O?",
                "correctAnswer":"Water",
                "options":["Water","Salt","Sugar","Ice"]
              },
              {
                "category":"History",
                "type":"boolean",
                "difficulty":"medium",
                "question":"Was Rome founded in 753 BC?",
                "correctAnswer":"True",
                "options":["True","False"]
              }
            ]
        """.trimIndent()
    }
}
