package com.basahero.elearning.data.local.dao

import androidx.room.*
import com.basahero.elearning.data.local.entity.*
import kotlinx.coroutines.flow.Flow

// ─────────────────────────────────────────────
// GradeLevelDao
// ─────────────────────────────────────────────

@Dao
interface GradeLevelDao {
    @Query("SELECT * FROM grade_level ORDER BY id ASC")
    fun observeAll(): Flow<List<GradeLevelEntity>>

    @Query("SELECT * FROM grade_level WHERE id = :id")
    suspend fun getById(id: Int): GradeLevelEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(grades: List<GradeLevelEntity>)
}

// ─────────────────────────────────────────────
// QuarterDao
// ─────────────────────────────────────────────

@Dao
interface QuarterDao {
    @Query("SELECT * FROM quarter WHERE grade_level_id = :gradeId ORDER BY quarter_number ASC")
    fun observeByGrade(gradeId: Int): Flow<List<QuarterEntity>>

    @Query("SELECT * FROM quarter WHERE id = :id")
    suspend fun getById(id: String): QuarterEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(quarters: List<QuarterEntity>)

    @Query("UPDATE quarter SET is_active = 1 WHERE id = :quarterId")
    suspend fun unlock(quarterId: String)
}

// ─────────────────────────────────────────────
// LessonDao
// ─────────────────────────────────────────────

@Dao
interface LessonDao {
    @Query("SELECT * FROM lesson WHERE quarter_id = :quarterId ORDER BY order_index ASC")
    fun observeByQuarter(quarterId: String): Flow<List<LessonEntity>>

    @Query("SELECT * FROM lesson WHERE id = :id")
    suspend fun getById(id: String): LessonEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertAll(lessons: List<LessonEntity>)
}

// ─────────────────────────────────────────────
// QuizDao
// ─────────────────────────────────────────────

@Dao
interface QuizDao {
    @Query("SELECT * FROM quiz_question WHERE lesson_id = :lessonId ORDER BY order_index ASC")
    suspend fun getQuestionsForLesson(lessonId: String): List<QuizQuestionEntity>

    @Query("SELECT * FROM quiz_choice WHERE question_id = :questionId ORDER BY order_index ASC")
    suspend fun getChoicesForQuestion(questionId: String): List<QuizChoiceEntity>

    // Fetch all choices for a list of question IDs in one query (avoids N+1)
    @Query("SELECT * FROM quiz_choice WHERE question_id IN (:questionIds)")
    suspend fun getChoicesForQuestions(questionIds: List<String>): List<QuizChoiceEntity>

    // Sum of all points_value for a lesson — used to calculate quiz_total
    @Query("SELECT COALESCE(SUM(points_value), 0) FROM quiz_question WHERE lesson_id = :lessonId")
    suspend fun getTotalPointsForLesson(lessonId: String): Int

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertQuestions(questions: List<QuizQuestionEntity>)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertChoices(choices: List<QuizChoiceEntity>)
}

// ─────────────────────────────────────────────
// StudentDao
// ─────────────────────────────────────────────

@Dao
interface StudentDao {
    // Login lookup — name + section must match exactly
    @Query("""
        SELECT * FROM student
        WHERE LOWER(full_name) = LOWER(:name)
          AND LOWER(section)   = LOWER(:section)
        LIMIT 1
    """)
    suspend fun findByNameAndSection(name: String, section: String): StudentEntity?

    @Query("SELECT * FROM student WHERE id = :id")
    suspend fun getById(id: String): StudentEntity?

    @Query("SELECT * FROM student WHERE class_id = :classId ORDER BY full_name ASC")
    fun observeByClass(classId: String): Flow<List<StudentEntity>>

    // Unsynced students that need to be pulled down from cloud
    @Query("SELECT * FROM student WHERE synced = 0")
    suspend fun getUnsynced(): List<StudentEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdate(student: StudentEntity)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertOrUpdateAll(students: List<StudentEntity>)

    @Query("UPDATE student SET last_active = :timestamp WHERE id = :studentId")
    suspend fun updateLastActive(studentId: String, timestamp: Long)

    @Query("UPDATE student SET synced = 1 WHERE id = :studentId")
    suspend fun markSynced(studentId: String)
}

// ─────────────────────────────────────────────
// ProgressDao
// ─────────────────────────────────────────────

@Dao
interface ProgressDao {
    // Get progress for all lessons in a quarter for one student
    @Query("""
        SELECT sp.* FROM student_progress sp
        INNER JOIN lesson l ON sp.lesson_id = l.id
        WHERE sp.student_id = :studentId AND l.quarter_id = :quarterId
        ORDER BY l.order_index ASC
    """)
    fun observeProgressForQuarter(studentId: String, quarterId: String): Flow<List<StudentProgressEntity>>

    @Query("SELECT * FROM student_progress WHERE student_id = :studentId AND lesson_id = :lessonId")
    suspend fun getProgress(studentId: String, lessonId: String): StudentProgressEntity?

    // Count DONE lessons in a quarter for a student — used for progress ring
    @Query("""
        SELECT COUNT(*) FROM student_progress sp
        INNER JOIN lesson l ON sp.lesson_id = l.id
        WHERE sp.student_id = :studentId
          AND l.quarter_id  = :quarterId
          AND sp.status      = 'DONE'
    """)
    fun observeDoneCount(studentId: String, quarterId: String): Flow<Int>

    // Total lessons in a quarter — denominator for the progress ring
    @Query("SELECT COUNT(*) FROM lesson WHERE quarter_id = :quarterId")
    fun observeTotalCount(quarterId: String): Flow<Int>

    // All unsynced progress rows — consumed by SyncProgressWorker
    @Query("SELECT * FROM student_progress WHERE synced = 0")
    suspend fun getUnsynced(): List<StudentProgressEntity>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insert(progress: StudentProgressEntity)

    // Retake: update score + status + retake_count but keep same row (unique constraint enforced)
    @Query("""
        UPDATE student_progress
        SET quiz_score    = :score,
            quiz_total    = :total,
            status        = :status,
            retake_count  = retake_count + 1,
            completed_at  = :completedAt,
            synced        = 0
        WHERE student_id = :studentId AND lesson_id = :lessonId
    """)
    suspend fun updateOnRetake(
        studentId:   String,
        lessonId:    String,
        score:       Int,
        total:       Int,
        status:      String,
        completedAt: Long?
    )

    @Query("UPDATE student_progress SET synced = 1 WHERE id = :id")
    suspend fun markSynced(id: String)

    // Bulk mark synced after WorkManager batch upload
    @Query("UPDATE student_progress SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)
}

// ─────────────────────────────────────────────
// PronunciationDao
// ─────────────────────────────────────────────

@Dao
interface PronunciationDao {
    @Query("""
        SELECT * FROM pronunciation_attempt
        WHERE student_id = :studentId AND lesson_id = :lessonId
        ORDER BY attempt_number ASC
    """)
    fun observeAttempts(studentId: String, lessonId: String): Flow<List<PronunciationAttemptEntity>>

    // Best attempt per lesson — shown on teacher dashboard
    @Query("""
        SELECT * FROM pronunciation_attempt
        WHERE student_id = :studentId AND lesson_id = :lessonId AND is_best = 1
        LIMIT 1
    """)
    suspend fun getBestAttempt(studentId: String, lessonId: String): PronunciationAttemptEntity?

    // Next attempt number for this student + lesson
    @Query("""
        SELECT COALESCE(MAX(attempt_number), 0) + 1
        FROM pronunciation_attempt
        WHERE student_id = :studentId AND lesson_id = :lessonId
    """)
    suspend fun nextAttemptNumber(studentId: String, lessonId: String): Int

    // Clear is_best flag before setting new best
    @Query("""
        UPDATE pronunciation_attempt SET is_best = 0
        WHERE student_id = :studentId AND lesson_id = :lessonId
    """)
    suspend fun clearBestFlag(studentId: String, lessonId: String)

    @Query("UPDATE pronunciation_attempt SET is_best = 1 WHERE id = :id")
    suspend fun setAsBest(id: String)

    @Query("SELECT * FROM pronunciation_attempt WHERE synced = 0")
    suspend fun getUnsynced(): List<PronunciationAttemptEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(attempt: PronunciationAttemptEntity)

    @Query("UPDATE pronunciation_attempt SET synced = 1 WHERE id IN (:ids)")
    suspend fun markSyncedBatch(ids: List<String>)
}