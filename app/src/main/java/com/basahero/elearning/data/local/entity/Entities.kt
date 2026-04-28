package com.basahero.elearning.data.local.entity

import androidx.room.ColumnInfo
import androidx.room.Entity
import androidx.room.ForeignKey
import androidx.room.Index
import androidx.room.PrimaryKey

// ─────────────────────────────────────────────
// CONTENT TABLES — seeded from JSON, read-only
// ─────────────────────────────────────────────

@Entity(tableName = "grade_level")
data class GradeLevelEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: Int,                    // 4, 5, or 6 — grade number IS the PK

    @ColumnInfo(name = "label")
    val label: String               // "Grade 4", "Grade 5", "Grade 6"
)

// ─────────────────────────────────────────────

@Entity(
    tableName = "quarter",
    foreignKeys = [
        ForeignKey(
            entity        = GradeLevelEntity::class,
            parentColumns = ["id"],
            childColumns  = ["grade_level_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("grade_level_id")]
)
data class QuarterEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "grade_level_id")
    val gradeLevelId: Int,          // FK → GRADE_LEVEL.id

    @ColumnInfo(name = "quarter_number")
    val quarterNumber: Int,         // 1, 2, 3, or 4

    @ColumnInfo(name = "title")
    val title: String,              // "Quarter 1", "Quarter 2"…

    @ColumnInfo(name = "is_active")
    val isActive: Boolean           // Q1 = true on seed, Q2–Q4 = false (locked)
)

// ─────────────────────────────────────────────

@Entity(
    tableName = "lesson",
    foreignKeys = [
        ForeignKey(
            entity        = QuarterEntity::class,
            parentColumns = ["id"],
            childColumns  = ["quarter_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("quarter_id")]
)
data class LessonEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "quarter_id")
    val quarterId: String,          // FK → QUARTER.id

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,            // Display order within quarter: 1, 2, 3…

    @ColumnInfo(name = "competency")
    val competency: String,         // MATATAG competency name e.g. "Inferring"

    @ColumnInfo(name = "title")
    val title: String,              // Lesson display title shown to student

    @ColumnInfo(name = "passage_text")
    val passageText: String,        // Full reading passage 400–600 words

    @ColumnInfo(name = "image_path")
    val imagePath: String?          // e.g. "assets/img/gr4_q1_inferring.webp" — nullable
)

// ─────────────────────────────────────────────

@Entity(
    tableName = "quiz_question",
    foreignKeys = [
        ForeignKey(
            entity        = LessonEntity::class,
            parentColumns = ["id"],
            childColumns  = ["lesson_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("lesson_id")]
)
data class QuizQuestionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "lesson_id")
    val lessonId: String,           // FK → LESSON.id

    @ColumnInfo(name = "question_text")
    val questionText: String,       // The question prompt shown to student

    @ColumnInfo(name = "question_type")
    val questionType: String,       // MCQ / FILL_IN / SEQUENCING / MATCHING

    @ColumnInfo(name = "order_index")
    val orderIndex: Int,            // Question order within lesson

    @ColumnInfo(name = "points_value")
    val pointsValue: Int = 1        // Default 1; matching/sequencing can be 2–3
)

// ─────────────────────────────────────────────

@Entity(
    tableName = "quiz_choice",
    foreignKeys = [
        ForeignKey(
            entity        = QuizQuestionEntity::class,
            parentColumns = ["id"],
            childColumns  = ["question_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("question_id")]
)
data class QuizChoiceEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "question_id")
    val questionId: String,         // FK → QUIZ_QUESTION.id

    @ColumnInfo(name = "choice_text")
    val choiceText: String,         // Answer option text

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,         // 1 true per MCQ; multiple for matching/sequencing

    @ColumnInfo(name = "order_index")
    val orderIndex: Int             // Display order — shuffled on render in UI
)

// ─────────────────────────────────────────────
// STUDENT TABLE — on-device + synced to cloud
// ─────────────────────────────────────────────

@Entity(tableName = "student")
data class StudentEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID — same on device and cloud

    @ColumnInfo(name = "class_id")
    val classId: String,            // References CLASS.id (cloud) — stored locally for context

    @ColumnInfo(name = "full_name")
    val fullName: String,           // Used for name-based login on student side

    @ColumnInfo(name = "section")
    val section: String,            // Used alongside name for login verification

    @ColumnInfo(name = "grade_level")
    val gradeLevel: Int,            // 4, 5, or 6 — determines lesson set shown

    @ColumnInfo(name = "last_active")
    val lastActive: Long?,          // epoch millis — updated on every app open

    @ColumnInfo(name = "synced")
    val synced: Boolean = false,    // false = not yet pulled from cloud to device

    @ColumnInfo(name = "created_at")
    val createdAt: Long             // epoch millis — used by WorkManager to detect new students
)

// ─────────────────────────────────────────────
// ACTIVITY TABLES — written on device, synced to cloud
// ─────────────────────────────────────────────

@Entity(
    tableName = "student_progress",
    foreignKeys = [
        ForeignKey(
            entity        = StudentEntity::class,
            parentColumns = ["id"],
            childColumns  = ["student_id"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = LessonEntity::class,
            parentColumns = ["id"],
            childColumns  = ["lesson_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [
        Index("student_id"),
        Index("lesson_id"),
        Index(value = ["student_id", "lesson_id"], unique = true) // prevents duplicate rows on retake
    ]
)
data class StudentProgressEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "student_id")
    val studentId: String,          // FK → STUDENT.id

    @ColumnInfo(name = "lesson_id")
    val lessonId: String,           // FK → LESSON.id

    @ColumnInfo(name = "status")
    val status: String,             // LOCKED / IN_PROGRESS / DONE

    @ColumnInfo(name = "quiz_score")
    val quizScore: Int = 0,         // Points earned e.g. 8

    @ColumnInfo(name = "quiz_total")
    val quizTotal: Int = 0,         // Max possible points e.g. 10

    @ColumnInfo(name = "retake_count")
    val retakeCount: Int = 0,       // How many times student has attempted this lesson

    @ColumnInfo(name = "completed_at")
    val completedAt: Long?,         // epoch millis — null if IN_PROGRESS

    @ColumnInfo(name = "synced")
    val synced: Boolean = false     // false = pending upload to cloud
)

// ─────────────────────────────────────────────

@Entity(
    tableName = "pronunciation_attempt",
    foreignKeys = [
        ForeignKey(
            entity        = StudentEntity::class,
            parentColumns = ["id"],
            childColumns  = ["student_id"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = LessonEntity::class,
            parentColumns = ["id"],
            childColumns  = ["lesson_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("student_id"), Index("lesson_id")]
)
data class PronunciationAttemptEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "student_id")
    val studentId: String,          // FK → STUDENT.id

    @ColumnInfo(name = "lesson_id")
    val lessonId: String,           // FK → LESSON.id

    @ColumnInfo(name = "attempt_number")
    val attemptNumber: Int,         // 1, 2, 3… increments per student per lesson

    @ColumnInfo(name = "score")
    val score: Int,                 // 0–100 from weighted Levenshtein algorithm

    @ColumnInfo(name = "is_best")
    val isBest: Boolean = false,    // true on highest scoring attempt — updated on each new attempt

    @ColumnInfo(name = "transcript")
    val transcript: String,         // Raw text Vosk heard from the student

    @ColumnInfo(name = "feedback_json")
    val feedbackJson: String,       // JSON: [{word:"cat", correct:true, confidence:0.92}, …]

    @ColumnInfo(name = "attempted_at")
    val attemptedAt: Long,          // epoch millis

    @ColumnInfo(name = "synced")
    val synced: Boolean = false     // false = pending upload to cloud
)

// ─────────────────────────────────────────────
// GAME TABLES — LAN during game, synced after
// ─────────────────────────────────────────────

@Entity(tableName = "game_session")
data class GameSessionEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID — shared with students over LAN as session identifier

    @ColumnInfo(name = "class_id")
    val classId: String,            // References CLASS.id (cloud)

    @ColumnInfo(name = "lesson_id")
    val lessonId: String,           // FK → LESSON.id — determines quiz questions used

    @ColumnInfo(name = "status")
    val status: String,             // WAITING / ACTIVE / DONE

    @ColumnInfo(name = "join_code")
    val joinCode: String,           // 4-digit code students enter to confirm session

    @ColumnInfo(name = "udp_port")
    val udpPort: Int = 8888,        // Teacher broadcasts IP on this port

    @ColumnInfo(name = "tcp_port")
    val tcpPort: Int = 8889,        // Students connect to teacher's TCP server on this port

    @ColumnInfo(name = "question_order")
    val questionOrder: String,      // JSON array of question UUIDs in shuffled order

    @ColumnInfo(name = "started_at")
    val startedAt: Long?,           // epoch millis — null before game starts

    @ColumnInfo(name = "ended_at")
    val endedAt: Long?,             // epoch millis — null while game is active

    @ColumnInfo(name = "synced")
    val synced: Boolean = false     // false = pending upload after game ends
)

// ─────────────────────────────────────────────

@Entity(
    tableName = "game_answer",
    foreignKeys = [
        ForeignKey(
            entity        = GameSessionEntity::class,
            parentColumns = ["id"],
            childColumns  = ["session_id"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = StudentEntity::class,
            parentColumns = ["id"],
            childColumns  = ["student_id"],
            onDelete      = ForeignKey.CASCADE
        ),
        ForeignKey(
            entity        = QuizQuestionEntity::class,
            parentColumns = ["id"],
            childColumns  = ["question_id"],
            onDelete      = ForeignKey.CASCADE
        )
    ],
    indices = [Index("session_id"), Index("student_id"), Index("question_id")]
)
data class GameAnswerEntity(
    @PrimaryKey
    @ColumnInfo(name = "id")
    val id: String,                 // UUID

    @ColumnInfo(name = "session_id")
    val sessionId: String,          // FK → GAME_SESSION.id

    @ColumnInfo(name = "student_id")
    val studentId: String,          // FK → STUDENT.id

    @ColumnInfo(name = "question_id")
    val questionId: String,         // FK → QUIZ_QUESTION.id

    @ColumnInfo(name = "answer_given")
    val answerGiven: String,        // Student's answer text or choice UUID

    @ColumnInfo(name = "is_correct")
    val isCorrect: Boolean,         // Evaluated against QUIZ_CHOICE.is_correct

    @ColumnInfo(name = "response_time_ms")
    val responseTimeMs: Int,        // ms from question shown to answer submitted (tiebreaker)

    @ColumnInfo(name = "synced")
    val synced: Boolean = false     // false = pending upload after game ends
)