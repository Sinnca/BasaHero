package com.basahero.elearning.data.repository

import com.basahero.elearning.data.local.AppDatabase
import com.basahero.elearning.data.local.SeedLessonPart
import com.basahero.elearning.data.local.entity.*
import com.basahero.elearning.data.model.*
import com.basahero.elearning.data.local.entity.AppConstants.PASS_THRESHOLD
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.json.Json
import java.time.Instant
import java.util.UUID
import com.basahero.elearning.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import android.util.Log

// Define an alias so Android Studio knows EXACTLY which LessonStatus to use
import com.basahero.elearning.data.local.entity.LessonStatus as DbLessonStatus

// ─────────────────────────────────────────────────────────────────────────────
// StudentRepository
// ─────────────────────────────────────────────────────────────────────────────
class StudentRepository(private val db: AppDatabase) {

    suspend fun loginStudent(fullName: String, section: String): Student? {
        // 1. Try local check first (for offline/speed)
        val localEntity = db.studentDao().findByNameAndSection(fullName, section)
        if (localEntity != null) {
            // Sync progress down in case they wiped data but remained logged in
            try { syncStudentDataDown(localEntity.id) } catch (e: Exception) {}
            return localEntity.toDomain()
        }

        // 2. Fallback to Supabase if not found locally (handles newly created students)
        return try {
            val remoteStudent = SupabaseClient.client
                .from("student")
                .select {
                    filter {
                        // Use ilike for case-insensitive matching
                        ilike("full_name", fullName)
                        ilike("section", section)
                    }
                }
                .decodeList<StudentRow>()
                .firstOrNull()

            if (remoteStudent != null) {
                // Save to Room for future offline access
                val entity = StudentEntity(
                    id = remoteStudent.id,
                    classId = remoteStudent.class_id ?: "",
                    fullName = remoteStudent.full_name,
                    section = remoteStudent.section,
                    gradeLevel = remoteStudent.grade_level,
                    lastActive = remoteStudent.last_active?.let { 
                        try { java.time.Instant.parse(it).toEpochMilli() } catch(e:Exception) { null } 
                    },
                    synced = true,
                    createdAt = remoteStudent.created_at?.let { 
                        try { java.time.Instant.parse(it).toEpochMilli() } catch(e:Exception) { System.currentTimeMillis() }
                    } ?: System.currentTimeMillis()
                )
                db.studentDao().insertOrUpdate(entity)
                
                // Sync progress down for newly downloaded students
                syncStudentDataDown(entity.id)
                
                entity.toDomain()
            } else {
                null
            }
        } catch (e: Exception) {
            Log.e("StudentRepository", "Supabase login failed: ${e.message}", e)
            null
        }
    }

    suspend fun getStudentById(studentId: String): Student? {
        return db.studentDao().getById(studentId)?.toDomain()
    }

    suspend fun getStudentsByGrade(gradeLevel: Int): List<Student> {
        try {
            val remoteStudents = SupabaseClient.client
                .from("student")
                .select { filter { eq("grade_level", gradeLevel) } }
                .decodeList<StudentRow>()
                
            val entities = remoteStudents.map { remoteStudent ->
                StudentEntity(
                    id = remoteStudent.id,
                    classId = remoteStudent.class_id ?: "",
                    fullName = remoteStudent.full_name,
                    section = remoteStudent.section,
                    gradeLevel = remoteStudent.grade_level,
                    lastActive = remoteStudent.last_active?.let { 
                        try { java.time.Instant.parse(it).toEpochMilli() } catch(e:Exception) { null } 
                    },
                    synced = true,
                    createdAt = remoteStudent.created_at?.let { 
                        try { java.time.Instant.parse(it).toEpochMilli() } catch(e:Exception) { System.currentTimeMillis() }
                    } ?: System.currentTimeMillis()
                )
            }
            db.studentDao().insertOrUpdateAll(entities)
        } catch (e: Exception) {
            Log.e("StudentRepository", "Supabase getStudentsByGrade failed: ${e.message}", e)
        }
        return db.studentDao().getByGradeLevel(gradeLevel).map { it.toDomain() }
    }

    suspend fun getClassesByGrade(gradeLevel: Int): List<ClassRow> {
        return try {
            SupabaseClient.client
                .from("class")
                .select { filter { eq("grade_level", gradeLevel) } }
                .decodeList<ClassRow>()
        } catch (e: Exception) {
            Log.e("StudentRepository", "Supabase getClassesByGrade failed: ${e.message}", e)
            emptyList()
        }
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

    suspend fun syncStudentDataDown(studentId: String) {
        try {
            // 1. Fetch pre_post_test
            val remotePrePost = SupabaseClient.client
                .from("pre_post_test")
                .select { filter { eq("student_id", studentId) } }
                .decodeList<PrePostTestRow>()

            // Delete local synced records that were deleted remotely
            val remotePrePostIds = remotePrePost.map { it.id }
            if (remotePrePostIds.isNotEmpty()) {
                db.prePostDao().deleteSyncedResultsNotIn(studentId, remotePrePostIds)
            } else {
                db.prePostDao().deleteAllSyncedResults(studentId)
            }

            remotePrePost.forEach { row ->
                try {
                    val entity = PrePostTestEntity(
                        id = row.id,
                        studentId = row.student_id,
                        quarterId = row.quarter_id,
                        testType = row.test_type,
                        score = row.score,
                        total = row.total,
                        completedAt = safeParseDate(row.completed_at),
                        synced = true
                    )
                    db.prePostDao().saveTestResult(entity)
                } catch (e: Exception) {
                    Log.w("StudentRepository", "Skipping orphaned pre-post record: ${row.id}")
                }
            }

            // 2. Fetch student_progress
            val remoteProgress = SupabaseClient.client
                .from("student_progress")
                .select { filter { eq("student_id", studentId) } }
                .decodeList<ProgressRow>()

            // Delete local synced records that were deleted remotely
            val remoteProgressIds = remoteProgress.map { it.id }
            if (remoteProgressIds.isNotEmpty()) {
                db.progressDao().deleteSyncedProgressNotIn(studentId, remoteProgressIds)
            } else {
                db.progressDao().deleteAllSyncedProgress(studentId)
            }

            remoteProgress.forEach { row ->
                try {
                    val entity = StudentProgressEntity(
                        id = row.id,
                        studentId = row.student_id,
                        lessonId = row.lesson_id,
                        status = row.status,
                        quizScore = row.quiz_score,
                        quizTotal = row.quiz_total,
                        firstScore = row.first_score,
                        bestScore = row.best_score ?: 0,
                        attemptCount = row.attempt_count ?: 1,
                        completedAt = row.completed_at?.let { safeParseDate(it) },
                        synced = true
                    )
                    db.progressDao().insertOrUpdateProgress(entity)
                } catch (e: Exception) {
                    Log.w("StudentRepository", "Skipping orphaned progress record: ${row.id}")
                }
            }
        } catch (e: Exception) {
            Log.e("StudentRepository", "Failed to sync student data down: ${e.message}")
        }
    }

    private fun safeParseDate(dateStr: String?): Long {
        if (dateStr.isNullOrBlank()) return System.currentTimeMillis()
        return try {
            val cleanStr = dateStr.replace("+00:00", "Z")
            java.time.Instant.parse(cleanStr).toEpochMilli()
        } catch (e: Exception) {
            System.currentTimeMillis()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// LessonRepository
// ─────────────────────────────────────────────────────────────────────────────
class LessonRepository(private val db: AppDatabase) {

    fun getQuartersWithProgress(gradeLevel: Int, studentId: String): Flow<List<Quarter>> {
        return kotlinx.coroutines.flow.combine(
            db.quarterDao().observeByGrade(gradeLevel),
            db.progressDao().getAllProgressForStudent(studentId)
        ) { quarters, _ ->
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
                    else -> DbLessonStatus.IN_PROGRESS
                }

                Lesson(
                    id = lesson.id,
                    quarterId = lesson.quarterId,
                    orderIndex = lesson.orderIndex,
                    competency = lesson.competency,
                    title = lesson.title,
                    passageText = lesson.passageText,
                    imagePath = lesson.imagePath,
                    status = status,
                    highlightedWords = lesson.highlightedWords,
                    lectureText = lesson.lectureText,
                    parts = parsePartsJson(lesson.partsJson)
                )
            }
        }
    }

    suspend fun getLessonById(lessonId: String, studentId: String? = null): Lesson? {
        val lesson = db.lessonDao().getById(lessonId) ?: return null
        val status = if (studentId != null) {
            db.progressDao().getProgress(studentId, lessonId)?.status ?: DbLessonStatus.IN_PROGRESS
        } else {
            DbLessonStatus.IN_PROGRESS
        }

        return Lesson(
            id = lesson.id,
            quarterId = lesson.quarterId,
            orderIndex = lesson.orderIndex,
            competency = lesson.competency,
            title = lesson.title,
            passageText = lesson.passageText,
            imagePath = lesson.imagePath,
            status = status,
            highlightedWords = lesson.highlightedWords,
            lectureText = lesson.lectureText,
            parts = parsePartsJson(lesson.partsJson)
        )
    }

    private val partsJsonParser = Json { ignoreUnknownKeys = true; isLenient = true }

    private fun parsePartsJson(jsonStr: String): List<LessonPart> {
        if (jsonStr.isBlank() || jsonStr == "[]") return emptyList()
        return try {
            val seedParts = partsJsonParser.decodeFromString(
                kotlinx.serialization.builtins.ListSerializer(SeedLessonPart.serializer()),
                jsonStr
            )
            seedParts.map { sp ->
                fun mapQuestions(questions: List<com.basahero.elearning.data.local.SeedMiniQuestion>) =
                    questions.map { mq ->
                        MiniQuestion(
                            id = mq.id,
                            questionText = mq.questionText,
                            questionType = mq.questionType,
                            choices = mq.choices.map { mc ->
                                MiniChoice(id = mc.id, choiceText = mc.choiceText, isCorrect = mc.isCorrect, orderIndex = mc.orderIndex)
                            }
                        )
                    }
                LessonPart(
                    passageText = sp.passageText,
                    imagePath = sp.imagePath,
                    highlightedWords = sp.highlighted_words,
                    miniQuestions = mapQuestions(sp.miniQuestions),
                    activityQuestions = mapQuestions(sp.activityQuestions)
                )
            }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    suspend fun getQuarterById(quarterId: String): Quarter? {
        return db.quarterDao().getById(quarterId)?.let { q ->
            Quarter(
                id = q.id,
                gradeLevelId = q.gradeLevelId,
                quarterNumber = q.quarterNumber,
                title = q.title,
                isActive = q.isActive,
                totalLessons = 0,
                completedLessons = 0
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
            id           = "${studentId}_${quarterId}_${testType}",
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

// ─────────────────────────────────────────────────────────────────────────────
// PronunciationRepository
// ─────────────────────────────────────────────────────────────────────────────
class PronunciationRepository(private val db: AppDatabase) {

    suspend fun savePronunciationAttempt(
        studentId: String,
        lessonId: String,
        word: String,
        heard: String,
        isCorrect: Boolean,
        score: Int
    ) {
        // Calculate the next attempt number
        val attemptNumber = db.pronunciationDao().nextAttemptNumber(studentId, lessonId)

        // Create feedback JSON
        val feedbackJson = """{"word": "$word", "heard": "$heard", "isCorrect": $isCorrect}"""

        // We only mark this as 'isBest' if they got it right, or if it's their first attempt
        val currentBest = db.pronunciationDao().getBestAttempt(studentId, lessonId)
        
        val isNewBest = currentBest == null || (score > currentBest.score)

        if (isNewBest) {
            db.pronunciationDao().clearBestFlag(studentId, lessonId)
        }

        val attempt = PronunciationAttemptEntity(
            id = UUID.randomUUID().toString(),
            studentId = studentId,
            lessonId = lessonId,
            attemptNumber = attemptNumber,
            score = score,
            isBest = isNewBest,
            transcript = word,
            feedbackJson = feedbackJson,
            attemptedAt = System.currentTimeMillis()
        )

        db.pronunciationDao().insert(attempt)
    }
}