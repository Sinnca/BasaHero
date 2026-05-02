package com.basahero.elearning.data.repository

import com.basahero.elearning.data.local.AppDatabase
import com.basahero.elearning.data.local.entity.*
import com.basahero.elearning.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// Define an alias so Android Studio knows EXACTLY which LessonStatus to use
import com.basahero.elearning.data.local.entity.LessonStatus as DbLessonStatus
import com.basahero.elearning.data.local.entity.AppConstants

// ─────────────────────────────────────────────────────────────────────────────
// StudentRepository
// ─────────────────────────────────────────────────────────────────────────────
class StudentRepository(private val db: AppDatabase) {

    suspend fun loginStudent(fullName: String, section: String): Student? {
        val entity = db.studentDao().findByNameAndSection(fullName, section)
        return entity?.toDomain()
    }

    suspend fun getStudentById(studentId: String): Student? {
        return db.studentDao().getById(studentId)?.toDomain()
    }

    suspend fun updateLastActive(studentId: String) {
        db.studentDao().updateLastActive(studentId, System.currentTimeMillis())
    }

    private fun StudentEntity.toDomain() = Student(
        id = id,
        classId = classId,
        fullName = fullName,
        section = section,
        gradeLevel = gradeLevel,
        lastActive = lastActive
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// LessonRepository
// ─────────────────────────────────────────────────────────────────────────────
class LessonRepository(private val db: AppDatabase) {

    fun getQuartersWithProgress(gradeLevel: Int, studentId: String): Flow<List<Quarter>> {
        return db.quarterDao().observeByGrade(gradeLevel).map { quarters ->
            quarters.map { q ->
                val total = db.lessonDao().observeByQuarter(q.id).first().size
                val completed = db.progressDao().observeDoneCount(studentId, q.id).first()
                Quarter(
                    id = q.id,
                    gradeLevelId = q.gradeLevelId,
                    quarterNumber = q.quarterNumber,
                    title = q.title,
                    isActive = q.isActive,
                    totalLessons = total,
                    completedLessons = completed
                )
            }
        }
    }

    fun getLessonsWithStatus(quarterId: String, studentId: String): Flow<List<Lesson>> {
        return db.lessonDao().observeByQuarter(quarterId).map { lessons ->
            var previousLessonDone = true

            lessons.map { lesson ->
                val progress = db.progressDao().getProgress(studentId, lesson.id)

                val status = when {
                    // Use explicitly imported DbLessonStatus to avoid ambiguity
                    progress != null -> progress.status
                    previousLessonDone -> DbLessonStatus.IN_PROGRESS
                    else -> DbLessonStatus.LOCKED
                }

                previousLessonDone = (progress?.status == DbLessonStatus.DONE)

                Lesson(
                    id = lesson.id,
                    quarterId = lesson.quarterId,
                    orderIndex = lesson.orderIndex,
                    competency = lesson.competency,
                    title = lesson.title,
                    passageText = lesson.passageText,
                    imagePath = lesson.imagePath,
                    status = status
                )
            }
        }
    }

    suspend fun getLessonById(lessonId: String): Lesson? {
        return db.lessonDao().getById(lessonId)?.let { lesson ->
            Lesson(
                id = lesson.id,
                quarterId = lesson.quarterId,
                orderIndex = lesson.orderIndex,
                competency = lesson.competency,
                title = lesson.title,
                passageText = lesson.passageText,
                imagePath = lesson.imagePath
            )
        }
    }

    suspend fun unlockQuarter(quarterId: String) {
        db.quarterDao().unlock(quarterId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuizRepository
// ─────────────────────────────────────────────────────────────────────────────
class QuizRepository(private val db: AppDatabase) {

    suspend fun getQuizForLesson(lessonId: String): List<QuizQuestion> {
        val questions = db.quizDao().getQuestionsForLesson(lessonId)

        val questionIds = questions.map { it.id }
        val allChoices = db.quizDao().getChoicesForQuestions(questionIds)

        val choicesByQuestion = allChoices.groupBy { it.questionId }

        return questions.map { q ->
            QuizQuestion(
                id = q.id,
                lessonId = q.lessonId,
                questionText = q.questionText,
                questionType = q.questionType,
                orderIndex = q.orderIndex,
                pointsValue = q.pointsValue,
                choices = (choicesByQuestion[q.id] ?: emptyList()).map { c ->
                    QuizChoice(
                        id = c.id,
                        questionId = c.questionId,
                        choiceText = c.choiceText,
                        isCorrect = c.isCorrect,
                        orderIndex = c.orderIndex
                    )
                }.sortedBy { it.orderIndex }
            )
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// ProgressRepository (PHASE 3B UPGRADED)
// ─────────────────────────────────────────────────────────────────────────────
class ProgressRepository(private val db: AppDatabase) {

    suspend fun getProgress(studentId: String, lessonId: String): StudentProgress? {
        return db.progressDao().getProgress(studentId, lessonId)?.toDomain()
    }

    suspend fun saveQuizResult(
        studentId: String,
        lessonId: String,
        score: Int,
        total: Int
    ) {
        val existing = db.progressDao().getProgress(studentId, lessonId)

        // Phase 3B: The 60% Pass Threshold Logic
        val isPassed = (score.toFloat() / total.toFloat()) >= AppConstants.PASS_THRESHOLD
        val newStatus = if (isPassed) DbLessonStatus.DONE else DbLessonStatus.IN_PROGRESS

        if (existing != null) {
            // Phase 3B: First Score & Best Score tracking
            val updatedProgress = existing.copy(
                quizScore = score,
                quizTotal = total,
                status = newStatus,
                bestScore = maxOf(existing.bestScore, score), // Keeps the highest score
                attemptCount = existing.attemptCount + 1,     // Increments attempts
                completedAt = System.currentTimeMillis(),
                synced = false
            )
            // Use the standard @Update DAO method we added!
            db.progressDao().update(updatedProgress)
        } else {
            // First time taking the quiz
            db.progressDao().insert(
                StudentProgressEntity(
                    id = UUID.randomUUID().toString(),
                    studentId = studentId,
                    lessonId = lessonId,
                    status = newStatus,
                    quizScore = score,
                    quizTotal = total,
                    firstScore = score, // Written once!
                    bestScore = score,
                    attemptCount = 1,
                    completedAt = System.currentTimeMillis(),
                    synced = false
                )
            )
        }
    }

    suspend fun markInProgress(studentId: String, lessonId: String) {
        val existing = db.progressDao().getProgress(studentId, lessonId)
        if (existing == null) {
            db.progressDao().insert(
                StudentProgressEntity(
                    id = UUID.randomUUID().toString(),
                    studentId = studentId,
                    lessonId = lessonId,
                    status = DbLessonStatus.IN_PROGRESS,
                    quizScore = 0,
                    quizTotal = 0,
                    firstScore = null,
                    bestScore = 0,
                    attemptCount = 0,
                    completedAt = null,
                    synced = false
                )
            )
        }
    }

    suspend fun getUnsyncedProgress(): List<StudentProgressEntity> {
        return db.progressDao().getUnsynced()
    }

    suspend fun markSynced(progressId: String) {
        db.progressDao().markSynced(progressId)
    }

    private fun StudentProgressEntity.toDomain() = StudentProgress(
        id = id,
        studentId = studentId,
        lessonId = lessonId,
        status = status,
        quizScore = quizScore,
        quizTotal = quizTotal,
        // Map Phase 3B attemptCount to your domain model
        retakeCount = attemptCount,
        completedAt = completedAt,
        synced = synced
    )
}