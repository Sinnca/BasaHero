package com.basahero.elearning.data.repository

import com.basahero.elearning.data.local.AppDatabase
import com.basahero.elearning.data.local.entity.*
import com.basahero.elearning.data.model.*
import com.basahero.elearning.data.local.entity.AppConstants.PASS_THRESHOLD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.time.Instant
import java.util.UUID

// Define an alias so Android Studio knows EXACTLY which LessonStatus to use
import com.basahero.elearning.data.local.entity.LessonStatus as DbLessonStatus

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
                // Uses the new query from Phase 3B ProgressDao
                val completed = db.progressDao().countCompletedLessonsInQuarter(studentId, q.id)
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

//    fun getLessonsWithStatus(quarterId: String, studentId: String): Flow<List<Lesson>> {
//        return db.lessonDao().observeByQuarter(quarterId).map { lessons ->
//            var previousLessonDone = true
//
//            lessons.map { lesson ->
//                val progress = db.progressDao().getProgress(studentId, lesson.id)
//
//                val status = when {
//                    progress != null -> progress.status
//                    previousLessonDone -> DbLessonStatus.IN_PROGRESS
//                    else -> DbLessonStatus.LOCKED
//                }
//
//                previousLessonDone = (progress?.status == DbLessonStatus.DONE)
//
//                Lesson(
//                    id = lesson.id,
//                    quarterId = lesson.quarterId,
//                    orderIndex = lesson.orderIndex,
//                    competency = lesson.competency,
//                    title = lesson.title,
//                    passageText = lesson.passageText,
//                    imagePath = lesson.imagePath,
//                    status = status,
//                    highlightedWords = lesson.highlightedWords // 👈 Pass it here
//                )
//            }
//        }
//    }

    // ✅ FIXED: Completely eliminates the "N+1" database query problem
    fun getLessonsWithStatus(quarterId: String, studentId: String): Flow<List<Lesson>> {
        // We grab the lessons and ALL progress at the same time
        return kotlinx.coroutines.flow.combine(
            db.lessonDao().observeByQuarter(quarterId),
            db.progressDao().getAllProgressForStudent(studentId)
        ) { lessons, allProgress ->
            var previousLessonDone = true

            lessons.map { lesson ->
                // Now we look it up in memory instead of hitting the database inside a loop!
                val progress = allProgress.find { it.lessonId == lesson.id }

                val status = when {
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
                    status = status,
                    highlightedWords = lesson.highlightedWords
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
                imagePath = lesson.imagePath,
                highlightedWords = lesson.highlightedWords // 👈 Pass it here
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
// ProgressRepository — Phase 3B updated
// ─────────────────────────────────────────────────────────────────────────────
class ProgressRepository(private val db: AppDatabase) {

    suspend fun getProgress(studentId: String, lessonId: String): StudentProgress? {
        return db.progressDao().getProgress(studentId, lessonId)?.toDomain()
    }

    fun getAllProgressForStudent(studentId: String): Flow<List<StudentProgress>> {
        return db.progressDao().getAllProgressForStudent(studentId).map { list ->
            list.map { it.toDomain() }
        }
    }

    // ── Save quiz result — core of Phase 3B Step 4 ───────────────────────────
    suspend fun saveQuizResult(
        studentId: String,
        lessonId: String,
        score: Int,
        total: Int
    ) {
        val existing = db.progressDao().getProgress(studentId, lessonId)
        val isFirstAttempt = existing == null || existing.firstScore == null

        // Determine pass/fail using 60% threshold
        val passed = total > 0 && (score.toFloat() / total) >= PASS_THRESHOLD
        val newStatus = if (passed) DbLessonStatus.DONE else DbLessonStatus.IN_PROGRESS

        val progress = StudentProgressEntity(
            id           = existing?.id ?: UUID.randomUUID().toString(),
            studentId    = studentId,
            lessonId     = lessonId,
            status       = newStatus,
            quizScore    = score,
            quizTotal    = total,
            // FIRST score — written once, never changed after first attempt
            firstScore   = if (isFirstAttempt) score else (existing?.firstScore ?: score),
            // BEST score — only updates if new score exceeds current best
            bestScore    = maxOf(score, existing?.bestScore ?: 0),
            // ATTEMPT COUNT — increments on every submission
            attemptCount = (existing?.attemptCount ?: 0) + 1,
            completedAt  = if (passed) System.currentTimeMillis() else existing?.completedAt,
            synced       = false
        )

        db.progressDao().insertOrUpdateProgress(progress)
    }

    suspend fun markInProgress(studentId: String, lessonId: String) {
        val existing = db.progressDao().getProgress(studentId, lessonId)
        if (existing?.status == DbLessonStatus.DONE) return

        val progress = StudentProgressEntity(
            id           = existing?.id ?: UUID.randomUUID().toString(),
            studentId    = studentId,
            lessonId     = lessonId,
            status       = DbLessonStatus.IN_PROGRESS,
            quizScore    = existing?.quizScore ?: 0,
            quizTotal    = existing?.quizTotal ?: 0,
            firstScore   = existing?.firstScore,
            bestScore    = existing?.bestScore ?: 0,
            attemptCount = existing?.attemptCount ?: 0,
            completedAt  = null,
            synced       = false
        )
        db.progressDao().insertOrUpdateProgress(progress)
    }

    suspend fun countCompletedInQuarter(studentId: String, quarterId: String): Int {
        return db.progressDao().countCompletedLessonsInQuarter(studentId, quarterId)
    }

    suspend fun getUnsyncedProgress() = db.progressDao().getUnsyncedProgress()
    suspend fun markSynced(progressId: String) = db.progressDao().markSynced(progressId)

    private fun StudentProgressEntity.toDomain() = StudentProgress(
        id           = id,
        studentId    = studentId,
        lessonId     = lessonId,
        status       = status,
        quizScore    = quizScore,
        quizTotal    = quizTotal,
        firstScore   = firstScore,
        bestScore    = bestScore,
        attemptCount = attemptCount,
        completedAt  = completedAt,
        synced       = synced
    )
}

// ─────────────────────────────────────────────────────────────────────────────
// PrePostRepository — NEW Phase 3B Step 5
// ─────────────────────────────────────────────────────────────────────────────
class PrePostRepository(private val db: AppDatabase) {

    suspend fun getTestQuestions(quarterId: String, testType: String): List<QuizQuestion> {
        val questions = db.prePostDao().getQuestions(quarterId, testType)
        val choices   = db.prePostDao().getChoicesForQuarter(quarterId, testType)
        val choiceMap = choices.groupBy { it.questionId }

        return questions.map { q ->
            QuizQuestion(
                id           = q.id,
                lessonId     = quarterId,
                questionText = q.questionText,
                questionType = q.questionType,
                orderIndex   = q.orderIndex,
                pointsValue  = q.pointsValue,
                choices      = (choiceMap[q.id] ?: emptyList()).map { c ->
                    QuizChoice(
                        id         = c.id,
                        questionId = c.questionId,
                        choiceText = c.choiceText,
                        isCorrect  = c.isCorrect,
                        orderIndex = c.orderIndex
                    )
                }.sortedBy { it.orderIndex }
            )
        }
    }

    suspend fun hasTestContent(quarterId: String, testType: String): Boolean {
        return db.prePostDao().countQuestions(quarterId, testType) > 0
    }

    suspend fun isTestCompleted(studentId: String, quarterId: String, testType: String): Boolean {
        return db.prePostDao().getTestResult(studentId, quarterId, testType) != null
    }

    suspend fun getTestResult(studentId: String, quarterId: String, testType: String): PrePostTestEntity? {
        return db.prePostDao().getTestResult(studentId, quarterId, testType)
    }

    suspend fun saveTestResult(
        studentId: String,
        quarterId: String,
        testType: String,
        score: Int,
        total: Int
    ) {
        val result = PrePostTestEntity(
            id           = UUID.randomUUID().toString(),
            studentId    = studentId,
            quarterId    = quarterId,
            testType     = testType,
            score        = score,
            total        = total,
            completedAt  = System.currentTimeMillis(),
            synced       = false
        )
        db.prePostDao().saveTestResult(result)
    }

    fun getAllResultsForStudent(studentId: String) =
        db.prePostDao().getAllResultsForStudent(studentId)
}