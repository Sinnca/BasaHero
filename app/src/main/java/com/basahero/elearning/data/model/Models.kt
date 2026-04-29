package com.basahero.elearning.data.model

// ─────────────────────────────────────────────────────────────────────────────
// Domain models — plain data classes, no Room or Supabase annotations
// These are what ViewModels and UI work with, NOT the raw Entity classes
// ─────────────────────────────────────────────────────────────────────────────

data class Student(
    val id: String,
    val classId: String,
    val fullName: String,
    val section: String,
    val gradeLevel: Int,
    val lastActive: Long?
)

data class Quarter(
    val id: String,
    val gradeLevelId: Int,
    val quarterNumber: Int,
    val title: String,
    val isActive: Boolean,
    // Computed fields added by repository
    val totalLessons: Int = 0,
    val completedLessons: Int = 0
) {
    val progressPercent: Float
        get() = if (totalLessons == 0) 0f else completedLessons.toFloat() / totalLessons
}

data class Lesson(
    val id: String,
    val quarterId: String,
    val orderIndex: Int,
    val competency: String,
    val title: String,
    val passageText: String,
    val imagePath: String?, // <--- FIXED: Added the ? so the app won't crash if an image is missing!
    // Status added by repository after checking STUDENT_PROGRESS
    val status: String = LessonStatus.LOCKED
) {
    val isLocked get() = status == LessonStatus.LOCKED
    val isDone get() = status == LessonStatus.DONE
    val isInProgress get() = status == LessonStatus.IN_PROGRESS
}

data class QuizQuestion(
    val id: String,
    val lessonId: String,
    val questionText: String,
    val questionType: String,
    val orderIndex: Int,
    val pointsValue: Int,
    val choices: List<QuizChoice> = emptyList()
)

data class QuizChoice(
    val id: String,
    val questionId: String,
    val choiceText: String,
    val isCorrect: Boolean,
    val orderIndex: Int
)

data class StudentProgress(
    val id: String,
    val studentId: String,
    val lessonId: String,
    val status: String,
    val quizScore: Int,
    val quizTotal: Int,
    val retakeCount: Int = 0, // <--- Added this to match your database schema
    val completedAt: Long?,
    val synced: Boolean
) {
    val scorePercent: Float
        get() = if (quizTotal == 0) 0f else quizScore.toFloat() / quizTotal
    val passed: Boolean
        get() = scorePercent >= 0.6f   // 60% passing threshold
}

// Quiz result passed between Quiz screen and Result screen
data class QuizResult(
    val lessonId: String,
    val lessonTitle: String,
    val score: Int,
    val total: Int,
    val passed: Boolean,
    val answeredQuestions: List<AnsweredQuestion>
)

data class AnsweredQuestion(
    val questionId: String,
    val questionText: String,
    val studentAnswer: String,
    val correctAnswer: String,
    val isCorrect: Boolean,
    val pointsEarned: Int,
    val pointsValue: Int
)

// Lesson status constants
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