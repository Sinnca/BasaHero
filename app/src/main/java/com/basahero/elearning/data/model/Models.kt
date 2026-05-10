package com.basahero.elearning.data.model

// ─────────────────────────────────────────────────────────────────────────────
// Domain models — plain data classes, no Room or Supabase annotations
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
    val imagePath: String?,
    val status: String = LessonStatus.LOCKED,
    val highlightedWords: String = "[]",
    val lectureText: String = "",
    val parts: List<LessonPart> = emptyList()
) {
    val isLocked get() = status == LessonStatus.LOCKED
    val isDone get() = status == LessonStatus.DONE
    val isInProgress get() = status == LessonStatus.IN_PROGRESS
    val hasLecture get() = lectureText.isNotBlank()
    val hasParts get() = parts.isNotEmpty()
}

/**
 * A single part/section of a lesson.
 * Each part has its own reading passage and mini-activity questions.
 * Reading passage + mini-activity are shown on the SAME page.
 */
data class LessonPart(
    val passageText: String,
    val highlightedWords: List<String> = emptyList(),
    val miniQuestions: List<MiniQuestion> = emptyList(),
    val activityQuestions: List<MiniQuestion> = emptyList() // Graded assessment per passage
)

data class MiniQuestion(
    val id: String,
    val questionText: String,
    val questionType: String = "MCQ",
    val choices: List<MiniChoice> = emptyList()
)

data class MiniChoice(
    val id: String,
    val choiceText: String,
    val isCorrect: Boolean,
    val orderIndex: Int
)

data class QuizQuestion(
    val id: String,
    val lessonId: String,
    val questionText: String,
    val questionType: String,
    val orderIndex: Int,
    val pointsValue: Int,
    val choices: List<QuizChoice> = emptyList(),
    val correctAnswerIds: List<String> = emptyList()
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
    val firstScore: Int?, // 👈 PHASE 3B: Added
    val bestScore: Int,   // 👈 PHASE 3B: Added
    val attemptCount: Int, // 👈 PHASE 3B: Replaces retakeCount
    val completedAt: Long?,
    val synced: Boolean
) {
    val scorePercent: Float
        get() = if (quizTotal == 0) 0f else quizScore.toFloat() / quizTotal
    val passed: Boolean
        get() = scorePercent >= 0.6f   // 60% passing threshold
}

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
    const val PASSAGE = "PASSAGE"   // Clickable highlighted-word span question
}