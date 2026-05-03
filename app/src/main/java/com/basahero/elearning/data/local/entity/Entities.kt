// ─────────────────────────────────────────────────────────────────────────────
// BASAHERO E-LEARNING — ROOM ENTITIES (Version 2)
// Phase 3B changes:
//   • LESSON         — + highlighted_words column
//   • STUDENT_PROGRESS — + first_score, best_score, attempt_count columns
//   • NEW: PRE_POST_QUESTION — pre-test and post-test questions (Room only)
//   • NEW: PRE_POST_TEST     — pre/post test results (Room + Supabase sync)
//   • PRONUNCIATION_ATTEMPT  — kept in Room, removed from Supabase sync
// Package: com.basahero.elearning.data.local.entity
// ─────────────────────────────────────────────────────────────────────────────

package com.basahero.elearning.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey


// ─────────────────────────────────────────────────────────────────────────────
// 1. GRADE_LEVEL — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Entity(tableName = "grade_level")
data class GradeLevelEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,

    @ColumnInfo(name = "label")
    val label: String
)


// ─────────────────────────────────────────────────────────────────────────────
// 2. QUARTER — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "quarter",
    foreignKeys = [
        ForeignKey(
            entity = GradeLevelEntity::class,
            parentColumns = ["id"],
            childColumns = ["grade_level_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("grade_level_id")]
)
data class QuarterEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "grade_level_id") val gradeLevelId: Int,
    @ColumnInfo(name = "quarter_number") val quarterNumber: Int,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "is_active") val isActive: Boolean
)


// ─────────────────────────────────────────────────────────────────────────────
// 3. LESSON — UPDATED: + highlighted_words
// highlighted_words is a JSON array of vocabulary words to highlight in passage.
// Format: [{"word":"cheerful","start":45,"end":53}]
// start/end = character positions in passage_text for AnnotatedString spans.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "lesson",
    foreignKeys = [
        ForeignKey(
            entity = QuarterEntity::class,
            parentColumns = ["id"],
            childColumns = ["quarter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quarter_id")]
)
data class LessonEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "quarter_id") val quarterId: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "competency") val competency: String,
    @ColumnInfo(name = "title") val title: String,
    @ColumnInfo(name = "passage_text") val passageText: String,
    @ColumnInfo(name = "image_path") val imagePath: String?,

    // NEW — Phase 3B Step 6
    // JSON array of highlighted vocabulary words with character positions
    // Empty string "[]" if no highlights defined for this lesson
    @ColumnInfo(name = "highlighted_words", defaultValue = "[]")
    val highlightedWords: String = "[]"
)


// ─────────────────────────────────────────────────────────────────────────────
// 4. QUIZ_QUESTION — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "quiz_question",
    foreignKeys = [
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lesson_id")]
)
data class QuizQuestionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "question_text") val questionText: String,
    @ColumnInfo(name = "question_type") val questionType: String,
    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "points_value") val pointsValue: Int = 1
)


// ─────────────────────────────────────────────────────────────────────────────
// 5. QUIZ_CHOICE — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "quiz_choice",
    foreignKeys = [
        ForeignKey(
            entity = QuizQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("question_id")]
)
data class QuizChoiceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "choice_text") val choiceText: String,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean,
    @ColumnInfo(name = "order_index") val orderIndex: Int
)


// ─────────────────────────────────────────────────────────────────────────────
// 6. STUDENT — unchanged
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "student",
    indices = [Index("class_id")]
)
data class StudentEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "class_id") val classId: String,
    @ColumnInfo(name = "full_name") val fullName: String,
    @ColumnInfo(name = "section") val section: String,
    @ColumnInfo(name = "grade_level") val gradeLevel: Int,
    @ColumnInfo(name = "last_active") val lastActive: Long?,
    @ColumnInfo(name = "synced") val synced: Boolean = false,
    @ColumnInfo(name = "created_at") val createdAt: Long
)


// ─────────────────────────────────────────────────────────────────────────────
// 7. STUDENT_PROGRESS — UPDATED: + first_score, best_score, attempt_count
//
// Three-score system:
//   first_score  — written ONCE on first attempt. NEVER overwritten.
//   best_score   — updated only when new score > current best.
//   quiz_score   — always the LATEST attempt score (existing column, renamed purpose).
//
// 60% pass rule:
//   status = DONE only when quiz_score / quiz_total >= 0.60
//   status = IN_PROGRESS when below 60% — student must retake
//
// attempt_count — increments on every quiz submission, including retakes.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "student_progress",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("student_id"),
        Index("lesson_id"),
        Index(value = ["student_id", "lesson_id"], unique = true)
    ]
)
data class StudentProgressEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "status") val status: String,
    @ColumnInfo(name = "quiz_score") val quizScore: Int = 0,
    @ColumnInfo(name = "quiz_total") val quizTotal: Int = 0,

    // FIXED: Changed to Int? = null so we can detect true first attempts!
    @ColumnInfo(name = "first_score") val firstScore: Int? = null,
    @ColumnInfo(name = "best_score", defaultValue = "0") val bestScore: Int = 0,
    @ColumnInfo(name = "attempt_count", defaultValue = "1") val attemptCount: Int = 1,
    @ColumnInfo(name = "retake_count") val retakeCount: Int = 0, // Restored to match v1 schema

    @ColumnInfo(name = "completed_at") val completedAt: Long? = null,
    @ColumnInfo(name = "synced") val synced: Boolean = false
)


// ─────────────────────────────────────────────────────────────────────────────
// 8. PRONUNCIATION_ATTEMPT — unchanged, Room only (NOT synced to Supabase)
// Removed from SyncPronunciationWorker to save Supabase free tier storage.
// 690 students × 63 lessons × avg 3 attempts × ~2KB = ~260MB — too large.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "pronunciation_attempt",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity = LessonEntity::class,
            parentColumns = ["id"],
            childColumns = ["lesson_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("student_id", name = "idx_pa_student"),
        Index("lesson_id", name = "idx_pa_lesson")
    ]
)
data class PronunciationAttemptEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "lesson_id") val lessonId: String,
    @ColumnInfo(name = "attempt_number") val attemptNumber: Int,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "is_best", defaultValue = "0") val isBest: Boolean = false,
    @ColumnInfo(name = "transcript") val transcript: String,
    @ColumnInfo(name = "feedback_json") val feedbackJson: String,
    @ColumnInfo(name = "attempted_at") val attemptedAt: Long
    // NOTE: synced column removed — this table never syncs to Supabase
)


// ─────────────────────────────────────────────────────────────────────────────
// 9. PRE_POST_QUESTION — NEW (Room only, seeded from JSON)
// Stores pre-test and post-test questions per quarter.
// Different questions from lesson quizzes — MCQ and FILL_IN only.
// test_type: "PRE" = pre-test, "POST" = post-test
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "pre_post_question",
    foreignKeys = [
        ForeignKey(
            entity = QuarterEntity::class,
            parentColumns = ["id"],
            childColumns = ["quarter_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quarter_id", name = "idx_ppq_quarter")]
)
data class PrePostQuestionEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,

    // Links to QUARTER — e.g. "q-gr4-q1"
    @ColumnInfo(name = "quarter_id") val quarterId: String,

    // "PRE" or "POST"
    @ColumnInfo(name = "test_type") val testType: String,

    @ColumnInfo(name = "question_text") val questionText: String,

    // MCQ or FILL_IN only — no SEQUENCING or MATCHING for pre/post tests
    @ColumnInfo(name = "question_type") val questionType: String,

    @ColumnInfo(name = "order_index") val orderIndex: Int,
    @ColumnInfo(name = "points_value", defaultValue = "1") val pointsValue: Int = 1
)


// ─────────────────────────────────────────────────────────────────────────────
// 10. PRE_POST_CHOICE — NEW (Room only, seeded from JSON)
// Answer choices for PRE_POST_QUESTION (same structure as QUIZ_CHOICE)
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "pre_post_choice",
    foreignKeys = [
        ForeignKey(
            entity = PrePostQuestionEntity::class,
            parentColumns = ["id"],
            childColumns = ["question_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [Index("question_id", name = "idx_ppc_question")]
)
data class PrePostChoiceEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "question_id") val questionId: String,
    @ColumnInfo(name = "choice_text") val choiceText: String,
    @ColumnInfo(name = "is_correct") val isCorrect: Boolean,
    @ColumnInfo(name = "order_index") val orderIndex: Int
)


// ─────────────────────────────────────────────────────────────────────────────
// 11. PRE_POST_TEST — NEW (Room + Supabase sync)
// Stores the completed pre-test or post-test result per student per quarter.
// UNIQUE(student_id, quarter_id, test_type) — one result per test per student.
// Synced to Supabase so teacher dashboard can compare PRE vs POST scores.
// ─────────────────────────────────────────────────────────────────────────────
@Entity(
    tableName = "pre_post_test",
    foreignKeys = [
        ForeignKey(
            entity = StudentEntity::class,
            parentColumns = ["id"],
            childColumns = ["student_id"],
            onDelete = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("student_id", name = "idx_ppt_student"),
        Index(value = ["student_id", "quarter_id", "test_type"], unique = true, name = "idx_ppt_unique")
    ]
)
data class PrePostTestEntity(
    @PrimaryKey @ColumnInfo(name = "id") val id: String,
    @ColumnInfo(name = "student_id") val studentId: String,
    @ColumnInfo(name = "quarter_id") val quarterId: String,
    @ColumnInfo(name = "test_type") val testType: String,
    @ColumnInfo(name = "score") val score: Int,
    @ColumnInfo(name = "total") val total: Int,

    // FIXED: Keep Room consistent by using Long. We will convert it to a String in the SyncWorker!
    @ColumnInfo(name = "completed_at") val completedAt: Long,
    @ColumnInfo(name = "synced", defaultValue = "0") val synced: Boolean = false
)


// ─────────────────────────────────────────────────────────────────────────────
// CONSTANTS — use everywhere instead of raw strings
// ─────────────────────────────────────────────────────────────────────────────
object LessonStatus {
    const val LOCKED = "LOCKED"
    const val IN_PROGRESS = "IN_PROGRESS"
    const val DONE = "DONE"
}

object QuestionType {
    const val MCQ = "MCQ"
    const val FILL_IN = "FILL_IN"
    const val SEQUENCING = "SEQUENCING"
    const val MATCHING = "MATCHING"
}

object TestType {
    const val PRE = "PRE"
    const val POST = "POST"
}

object AppConstants {
    // 60% minimum score to pass a lesson and unlock the next one
    const val PASS_THRESHOLD = 0.60f

    // Supabase free tier protection — pronunciation never syncs to cloud
    const val PRONUNCIATION_SYNC_ENABLED = false
}