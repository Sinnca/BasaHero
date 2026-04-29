package com.basahero.elearning.data.repository

import com.basahero.elearning.data.local.AppDatabase
import com.basahero.elearning.data.local.entity.*
import com.basahero.elearning.data.model.*
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID

// ─────────────────────────────────────────────────────────────────────────────
// StudentRepository
// ─────────────────────────────────────────────────────────────────────────────
class StudentRepository(private val db: AppDatabase) {

    suspend fun loginStudent(fullName: String, section: String): Student? {
        val entity = db.studentDao().findByNameAndSection(fullName, section)
        return entity?.toDomain()
    }

    suspend fun getStudentById(studentId: String): Student? {
        // FIXED: Matched DAO's getById()
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
        // FIXED: Matched DAO's observeByGrade() and used first() to safely read counts
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
        // FIXED: Matched DAO's observeByQuarter()
        return db.lessonDao().observeByQuarter(quarterId).map { lessons ->
            lessons.mapIndexed { index, lesson ->
                val progress = db.progressDao().getProgress(studentId, lesson.id)
                val status = progress?.status ?: if (index == 0) LessonStatus.IN_PROGRESS else LessonStatus.LOCKED

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
        // FIXED: Matched DAO's getById()
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
        // FIXED: Matched DAO's unlock()
        db.quarterDao().unlock(quarterId)
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// QuizRepository
// ─────────────────────────────────────────────────────────────────────────────
class QuizRepository(private val db: AppDatabase) {

    suspend fun getQuizForLesson(lessonId: String): List<QuizQuestion> {
        // FIXED: Matched DAO's getQuestionsForLesson() and getChoicesForQuestions()
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
// ProgressRepository
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
        if (existing != null) {
            // FIXED: Used DAO's specialized updateOnRetake() function
            db.progressDao().updateOnRetake(
                studentId = studentId,
                lessonId = lessonId,
                score = score,
                total = total,
                status = LessonStatus.DONE,
                completedAt = System.currentTimeMillis()
            )
        } else {
            // FIXED: Used standard insert
            db.progressDao().insert(
                StudentProgressEntity(
                    id = UUID.randomUUID().toString(),
                    studentId = studentId,
                    lessonId = lessonId,
                    status = LessonStatus.DONE,
                    quizScore = score,
                    quizTotal = total,
                    retakeCount = 1,
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
                    status = LessonStatus.IN_PROGRESS,
                    quizScore = 0,
                    quizTotal = 0,
                    retakeCount = 0,
                    completedAt = null,
                    synced = false
                )
            )
        }
    }

    suspend fun getUnsyncedProgress(): List<StudentProgressEntity> {
        // FIXED: Matched DAO's getUnsynced()
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
        retakeCount = retakeCount,
        completedAt = completedAt,
        synced = synced
    )
}