package com.basahero.elearning.data.repository

import android.util.Log
import com.basahero.elearning.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID

data class GameSession(
    val id: String,
    val classId: String,
    val lessonId: String,
    val status: String,
    val joinCode: String,
    val currentQuestionId: String?,
    val questionOrder: List<String>,
    val questionIndex: Int,
    val startedAt: String?,
    val endedAt: String?
)

data class GameAnswer(
    val id: String,
    val sessionId: String,
    val studentId: String,
    val questionId: String,
    val answerGiven: String,
    val isCorrect: Boolean,
    val responseTimeMs: Int,
    val answeredAt: String?
)

@Serializable
data class GameSessionRow(
    val id: String,
    val class_id: String,
    val lesson_id: String,
    val status: String,
    val join_code: String,
    val current_question_id: String? = null,
    val question_order: String,
    val question_index: Int,
    val started_at: String? = null,
    val ended_at: String? = null
)

@Serializable
data class GameAnswerRow(
    val id: String,
    val session_id: String,
    val student_id: String,
    val question_id: String,
    val answer_given: String,
    val is_correct: Boolean,
    val response_time_ms: Int,
    val answered_at: String? = null
)

// Serializable update payloads for Supabase
@Serializable
data class SessionStatusUpdate(val status: String)

@Serializable
data class SessionAdvanceUpdate(
    val current_question_id: String,
    val question_index: Int,
    val status: String
)

@Serializable
data class SessionEndUpdate(
    val status: String,
    val ended_at: String
)

class GameRepository {
    private val tag = "GameRepository"

    companion object {
        // A fixed nil-UUID used as the question_id when a student "joins" the lobby.
        // We can't use the string "JOIN" because the column is UUID-typed in Supabase.
        const val JOIN_MARKER_ID = "00000000-0000-0000-0000-000000000000"
    }

    private fun GameSessionRow.toDomain(): GameSession {
        val order = try {
            Json.decodeFromString<List<String>>(question_order)
        } catch (e: Exception) { emptyList() }
        return GameSession(id, class_id, lesson_id, status, join_code, current_question_id, order, question_index, started_at, ended_at)
    }

    private fun GameAnswerRow.toDomain() = GameAnswer(
        id, session_id, student_id, question_id, answer_given, is_correct, response_time_ms, answered_at
    )

    suspend fun createGameSession(classId: String, lessonId: String, questionIds: List<String>): GameSession {
        val joinCode = String.format("%04d", (0..9999).random())
        val shuffled = questionIds.shuffled()
        val orderJson = Json.encodeToString(shuffled)
        val row = GameSessionRow(
            id = UUID.randomUUID().toString(),
            class_id = classId,
            lesson_id = lessonId,
            status = "WAITING",
            join_code = joinCode,
            question_order = orderJson,
            question_index = 0,
            started_at = Instant.now().toString()
        )
        SupabaseClient.client.from("game_session").insert(row)
        Log.d(tag, "Created game session: ${row.id} with code: $joinCode")
        return row.toDomain()
    }

    suspend fun getSessionByJoinCode(code: String): GameSession? {
        return try {
            val sessions = SupabaseClient.client.from("game_session")
                .select { 
                    filter { eq("join_code", code) }
                }
                .decodeList<GameSessionRow>()
            val result = sessions.firstOrNull { it.status == "WAITING" || it.status == "ACTIVE" }?.toDomain()
            Log.d(tag, "getSessionByJoinCode($code): found=${result != null}")
            result
        } catch (e: Exception) {
            Log.e(tag, "getSessionByJoinCode error: ${e.message}")
            null
        }
    }

    suspend fun updateSessionStatus(sessionId: String, status: String) {
        try {
            SupabaseClient.client.from("game_session").update(
                SessionStatusUpdate(status = status)
            ) { filter { eq("id", sessionId) } }
            Log.d(tag, "Updated session $sessionId status to $status")
        } catch (e: Exception) { Log.e(tag, "updateSessionStatus error: ${e.message}") }
    }

    suspend fun advanceQuestion(sessionId: String, questionId: String, index: Int) {
        try {
            SupabaseClient.client.from("game_session").update(
                SessionAdvanceUpdate(
                    current_question_id = questionId,
                    question_index = index,
                    status = "ACTIVE"
                )
            ) { filter { eq("id", sessionId) } }
            Log.d(tag, "Advanced session $sessionId to question $questionId at index $index")
        } catch (e: Exception) { Log.e(tag, "advanceQuestion error: ${e.message}") }
    }

    suspend fun endSession(sessionId: String) {
        try {
            SupabaseClient.client.from("game_session").update(
                SessionEndUpdate(
                    status = "DONE",
                    ended_at = Instant.now().toString()
                )
            ) { filter { eq("id", sessionId) } }
            Log.d(tag, "Ended session $sessionId")
        } catch (e: Exception) { Log.e(tag, "endSession error: ${e.message}") }
    }

    suspend fun deleteSession(sessionId: String) {
        try {
            SupabaseClient.client.from("game_session").delete {
                filter { eq("id", sessionId) }
            }
            Log.d(tag, "Deleted unused session $sessionId")
        } catch (e: Exception) { Log.e(tag, "deleteSession error: ${e.message}") }
    }

    suspend fun submitAnswer(sessionId: String, studentId: String, questionId: String, answerGiven: String, isCorrect: Boolean, responseTimeMs: Int) {
        try {
            val row = GameAnswerRow(
                id = UUID.randomUUID().toString(),
                session_id = sessionId,
                student_id = studentId,
                question_id = questionId,
                answer_given = answerGiven,
                is_correct = isCorrect,
                response_time_ms = responseTimeMs,
                answered_at = Instant.now().toString()
            )
            SupabaseClient.client.from("game_answer").insert(row)
            Log.d(tag, "Submitted answer: student=$studentId, question=$questionId, session=$sessionId")
        } catch (e: Exception) { Log.e(tag, "submitAnswer error: ${e.message}") }
    }

    // ── Simple polling-based observation ─────────────────────────────────────
    // Polls every 2 seconds. Reliable regardless of Realtime table config.

    fun observeSession(sessionId: String): Flow<GameSession> = flow {
        Log.d(tag, "observeSession STARTED for $sessionId")
        while (true) {
            try {
                val row = SupabaseClient.client.from("game_session")
                    .select { filter { eq("id", sessionId) } }
                    .decodeSingleOrNull<GameSessionRow>()
                if (row != null) {
                    emit(row.toDomain())
                }
            } catch (e: Exception) {
                Log.e(tag, "observeSession poll error: ${e.message}")
            }
            delay(2000)
        }
    }

    fun observeAnswers(sessionId: String): Flow<List<GameAnswer>> = flow {
        Log.d(tag, "observeAnswers STARTED for $sessionId")
        while (true) {
            try {
                val rows = SupabaseClient.client.from("game_answer")
                    .select { filter { eq("session_id", sessionId) } }
                    .decodeList<GameAnswerRow>()
                Log.d(tag, "observeAnswers poll: ${rows.size} answers found")
                emit(rows.map { it.toDomain() })
            } catch (e: Exception) {
                Log.e(tag, "observeAnswers poll error: ${e.message}")
            }
            delay(2000)
        }
    }
}
