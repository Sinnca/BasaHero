package com.basahero.elearning.data.repository

import android.util.Log
import com.basahero.elearning.data.remote.SupabaseClient
import io.github.jan.supabase.auth.auth
import io.github.jan.supabase.auth.providers.builtin.Email
import io.github.jan.supabase.postgrest.from
import io.github.jan.supabase.postgrest.query.Order
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

// ─────────────────────────────────────────────────────────────────────────────
// Data classes matching Supabase cloud tables
// ─────────────────────────────────────────────────────────────────────────────

@Serializable
data class TeacherRow(
    val id: String,
    val email: String,
    val full_name: String,
    val created_at: String? = null
)

@Serializable
data class ClassRow(
    val id: String,
    val teacher_id: String,
    val name: String,
    val grade_level: Int,
    val school_year: String
)

@Serializable
data class StudentRow(
    val id: String,
    val class_id: String? = null,
    val full_name: String,
    val section: String,
    val grade_level: Int,
    val last_active: String? = null,
    val created_at: String? = null
)

@Serializable
data class ProgressRow(
    val id: String,
    val student_id: String,
    val lesson_id: String,
    val status: String,
    val quiz_score: Int,
    val quiz_total: Int,
    val first_score: Int? = null,
    val best_score: Int? = null,
    val attempt_count: Int? = null,
    val completed_at: String? = null
)

@Serializable
data class PrePostTestRow(
    val id: String,
    val student_id: String,
    val quarter_id: String,
    val test_type: String,          // "PRE" | "POST"
    val score: Int,
    val total: Int,
    val completed_at: String? = null
)

// Domain models for UI
data class TeacherProfile(
    val id: String,
    val email: String,
    val fullName: String
)

data class ClassInfo(
    val id: String,
    val teacherId: String,
    val name: String,
    val gradeLevel: Int,
    val schoolYear: String,
    val studentCount: Int = 0
)

data class StudentInfo(
    val id: String,
    val classId: String,
    val fullName: String,
    val section: String,
    val gradeLevel: Int,
    val lastActive: String?,
    val isAtRisk: Boolean = false
)

data class StudentProgressSummary(
    val studentId: String,
    val studentName: String,
    val lessonId: String,
    val lessonTitle: String,
    val competency: String,
    val status: String,
    val latestScore: Int,
    val firstScore: Int,
    val bestScore: Int,
    val quizTotal: Int,
    val attemptCount: Int,
    val isAtRisk: Boolean
) {
    val bestPercent: Float get() = if (quizTotal == 0) 0f else bestScore.toFloat() / quizTotal
    val firstPercent: Float get() = if (quizTotal == 0) 0f else firstScore.toFloat() / quizTotal
    val improvement: Int get() = bestScore - firstScore
}

data class PrePostComparison(
    val studentId: String,
    val studentName: String,
    val quarterId: String,
    val preScore: Int?,
    val preTotal: Int?,
    val postScore: Int?,
    val postTotal: Int?
) {
    val prePercent: Float get() = if ((preTotal ?: 0) == 0) 0f
        else (preScore ?: 0).toFloat() / preTotal!!
    val postPercent: Float get() = if ((postTotal ?: 0) == 0) 0f
        else (postScore ?: 0).toFloat() / postTotal!!
    val improvement: Float get() = postPercent - prePercent
}

data class LessonPerformance(
    val lessonId: String,
    val lessonTitle: String,
    val competency: String,
    val averageScore: Float,
    val passCount: Int,
    val failCount: Int,
    val notAttempted: Int,
    val totalStudents: Int
) {
    val passRate: Float get() = if (totalStudents == 0) 0f else passCount.toFloat() / totalStudents
    val isLowPerforming: Boolean get() = passRate < 0.6f
}


// ─────────────────────────────────────────────────────────────────────────────
// TeacherAuthRepository — login, signup, session
// ─────────────────────────────────────────────────────────────────────────────
class TeacherAuthRepository {

    private val tag = "TeacherAuthRepository"

    suspend fun signIn(email: String, password: String): Result<TeacherProfile> {
        return try {
            SupabaseClient.client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            val user = SupabaseClient.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Login failed — no user returned"))

            // Fetch teacher profile from public.teacher table
            val teacher = SupabaseClient.client
                .from("teacher")
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<TeacherRow>()

            if (teacher != null) {
                Result.success(TeacherProfile(teacher.id, teacher.email, teacher.full_name))
            } else {
                // First time — create teacher profile row
                val newTeacher = TeacherRow(
                    id = user.id,
                    email = email,
                    full_name = email.substringBefore("@")
                )
                SupabaseClient.client.from("teacher").insert(newTeacher)
                Result.success(TeacherProfile(user.id, email, newTeacher.full_name))
            }
        } catch (e: Exception) {
            Log.e(tag, "Sign in failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun signUp(email: String, password: String, fullName: String): Result<TeacherProfile> {
        return try {
            SupabaseClient.client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            val user = SupabaseClient.client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("Signup failed"))

            val newTeacher = TeacherRow(id = user.id, email = email, full_name = fullName)
            SupabaseClient.client.from("teacher").insert(newTeacher)
            Result.success(TeacherProfile(user.id, email, fullName))
        } catch (e: Exception) {
            Log.e(tag, "Sign up failed: ${e.message}")
            Result.failure(e)
        }
    }

    suspend fun getCurrentTeacher(): TeacherProfile? {
        return try {
            val user = SupabaseClient.client.auth.currentUserOrNull() ?: return null
            val teacher = SupabaseClient.client
                .from("teacher")
                .select { filter { eq("id", user.id) } }
                .decodeSingleOrNull<TeacherRow>() ?: return null
            TeacherProfile(teacher.id, teacher.email, teacher.full_name)
        } catch (e: Exception) {
            Log.e(tag, "Get current teacher failed: ${e.message}")
            null
        }
    }

    suspend fun signOut() {
        try { SupabaseClient.client.auth.signOut() }
        catch (e: Exception) { Log.e(tag, "Sign out error: ${e.message}") }
    }

    fun isLoggedIn(): Boolean {
        return SupabaseClient.client.auth.currentUserOrNull() != null
    }
}


// ─────────────────────────────────────────────────────────────────────────────
// ClassRepository — class and student management
// ─────────────────────────────────────────────────────────────────────────────
class ClassRepository {

    private val tag = "ClassRepository"

    suspend fun getClassesForTeacher(teacherId: String): List<ClassInfo> {
        return try {
            val classes = SupabaseClient.client
                .from("class")
                .select { filter { eq("teacher_id", teacherId) } }
                .decodeList<ClassRow>()

            classes.map { c ->
                val count = SupabaseClient.client
                    .from("student")
                    .select { filter { eq("class_id", c.id) } }
                    .decodeList<StudentRow>().size

                ClassInfo(c.id, c.teacher_id, c.name, c.grade_level, c.school_year, count)
            }
        } catch (e: Exception) {
            Log.e(tag, "Get classes failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun createClass(teacherId: String, name: String, gradeLevel: Int, schoolYear: String): Result<ClassInfo> {
        return try {
            val id = java.util.UUID.randomUUID().toString()
            val row = ClassRow(id, teacherId, name, gradeLevel, schoolYear)
            SupabaseClient.client.from("class").insert(row)
            Result.success(ClassInfo(id, teacherId, name, gradeLevel, schoolYear, 0))
        } catch (e: Exception) {
            Log.e(tag, "Create class failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Get students paginated — 20 per page
    suspend fun getStudentsForClass(classId: String, page: Int = 0): List<StudentInfo> {
        return try {
            val limit = 20
            val offset = page * limit

            val students = SupabaseClient.client
                .from("student")
                .select {
                    filter { eq("class_id", classId) }
                    order("full_name", Order.ASCENDING)
                    range(offset.toLong(), offset.toLong() + limit.toLong() - 1L)
                }
                .decodeList<StudentRow>()

            students.map { s ->
                // Check if student is at-risk (best_score < 60% on any completed lesson)
                val progressRows = SupabaseClient.client
                    .from("student_progress")
                    .select { filter { eq("student_id", s.id) } }
                    .decodeList<ProgressRow>()

                val isAtRisk = progressRows.any { p ->
                    p.status == "DONE" && p.best_score != null && p.quiz_total > 0 &&
                    (p.best_score.toFloat() / p.quiz_total) < 0.6f
                }

                StudentInfo(s.id, s.class_id ?: "", s.full_name, s.section, s.grade_level, s.last_active, isAtRisk)
            }
        } catch (e: Exception) {
            Log.e(tag, "Get students failed: ${e.message}")
            emptyList()
        }
    }

    suspend fun addStudent(classId: String, fullName: String, section: String, gradeLevel: Int): Result<StudentInfo> {
        return try {
            val id = java.util.UUID.randomUUID().toString()
            val now = System.currentTimeMillis()
            val row = StudentRow(
                id = id,
                class_id = classId,
                full_name = fullName.trim(),
                section = section.trim(),
                grade_level = gradeLevel,
                created_at = java.time.Instant.ofEpochMilli(now).toString()
            )
            SupabaseClient.client.from("student").insert(row)
            Result.success(StudentInfo(id, classId, fullName, section, gradeLevel, null))
        } catch (e: Exception) {
            Log.e(tag, "Add student failed: ${e.message}")
            Result.failure(e)
        }
    }

    // Bulk import students from CSV
    suspend fun importStudentsFromCsv(classId: Int, gradeLevel: Int, rows: List<CsvStudentRow>): Result<Int> {
        return try {
            val studentRows = rows.map { csv ->
                val now = java.time.Instant.now().toString()
                StudentRow(
                    id = java.util.UUID.randomUUID().toString(),
                    class_id = classId.toString(),
                    full_name = csv.fullName.trim(),
                    section = csv.section.trim(),
                    grade_level = gradeLevel,
                    created_at = now
                )
            }
            // Batch insert in chunks of 50
            studentRows.chunked(50).forEach { chunk ->
                SupabaseClient.client.from("student").insert(chunk)
            }
            Result.success(studentRows.size)
        } catch (e: Exception) {
            Log.e(tag, "CSV import failed: ${e.message}")
            Result.failure(e)
        }
    }
}

data class CsvStudentRow(val fullName: String, val section: String)


// ─────────────────────────────────────────────────────────────────────────────
// ProgressMonitorRepository — progress + pre/post data for teacher dashboard
// ─────────────────────────────────────────────────────────────────────────────
class ProgressMonitorRepository {

    private val tag = "ProgressMonitorRepository"

    // Get all progress for a specific student — with first/best/latest scores
    suspend fun getStudentProgress(studentId: String): List<StudentProgressSummary> {
        return try {
            val progressRows = SupabaseClient.client
                .from("student_progress")
                .select { filter { eq("student_id", studentId) } }
                .decodeList<ProgressRow>()

            progressRows.map { p ->
                val isAtRisk = p.status == "DONE" && p.quiz_total > 0 &&
                    ((p.best_score ?: 0).toFloat() / p.quiz_total) < 0.6f

                StudentProgressSummary(
                    studentId = p.student_id,
                    studentName = "",           // Filled by ViewModel from student lookup
                    lessonId = p.lesson_id,
                    lessonTitle = p.lesson_id,  // ViewModel resolves from Room
                    competency = "",
                    status = p.status,
                    latestScore = p.quiz_score,
                    firstScore = p.first_score ?: 0,
                    bestScore = p.best_score ?: p.quiz_score,
                    quizTotal = p.quiz_total,
                    attemptCount = p.attempt_count ?: 1,
                    isAtRisk = isAtRisk
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Get student progress failed: ${e.message}")
            emptyList()
        }
    }

    // Pre vs Post comparison for all students in a class per quarter
    suspend fun getPrePostComparison(classId: String, quarterId: String): List<PrePostComparison> {
        return try {
            // Get all students in class
            val students = SupabaseClient.client
                .from("student")
                .select { filter { eq("class_id", classId) } }
                .decodeList<StudentRow>()

            students.map { student ->
                // Get pre-test and post-test for this student and quarter
                val tests = SupabaseClient.client
                    .from("pre_post_test")
                    .select {
                        filter {
                            eq("student_id", student.id)
                            eq("quarter_id", quarterId)
                        }
                    }
                    .decodeList<PrePostTestRow>()

                val pre = tests.firstOrNull { it.test_type == "PRE" }
                val post = tests.firstOrNull { it.test_type == "POST" }

                PrePostComparison(
                    studentId = student.id,
                    studentName = student.full_name,
                    quarterId = quarterId,
                    preScore = pre?.score,
                    preTotal = pre?.total,
                    postScore = post?.score,
                    postTotal = post?.total
                )
            }
        } catch (e: Exception) {
            Log.e(tag, "Get pre/post comparison failed: ${e.message}")
            emptyList()
        }
    }

    // Class-wide lesson performance — which lessons have low scores
    suspend fun getLessonPerformance(classId: String): List<LessonPerformance> {
        return try {
            val students = SupabaseClient.client
                .from("student")
                .select { filter { eq("class_id", classId) } }
                .decodeList<StudentRow>()

            val studentIds = students.map { it.id }
            val totalStudents = students.size

            // Get all progress rows for the class
            val allProgress = mutableListOf<ProgressRow>()
            studentIds.chunked(50).forEach { chunk ->
                val rows = SupabaseClient.client
                    .from("student_progress")
                    .select { filter { isIn("student_id", chunk) } }
                    .decodeList<ProgressRow>()
                allProgress.addAll(rows)
            }

            // Group by lesson
            val byLesson = allProgress.groupBy { it.lesson_id }

            byLesson.map { (lessonId, progressList) ->
                val attempted = progressList.filter { it.status == "DONE" }
                val passCount = attempted.count { p ->
                    p.quiz_total > 0 && (p.best_score ?: p.quiz_score).toFloat() / p.quiz_total >= 0.6f
                }
                val failCount = attempted.size - passCount
                val avgScore = if (attempted.isEmpty()) 0f else
                    attempted.map { p ->
                        if (p.quiz_total == 0) 0f
                        else (p.best_score ?: p.quiz_score).toFloat() / p.quiz_total
                    }.average().toFloat()

                LessonPerformance(
                    lessonId = lessonId,
                    lessonTitle = lessonId,     // ViewModel resolves from Room
                    competency = "",
                    averageScore = avgScore,
                    passCount = passCount,
                    failCount = failCount,
                    notAttempted = totalStudents - attempted.size,
                    totalStudents = totalStudents
                )
            }.sortedBy { it.averageScore }     // Lowest performing first
        } catch (e: Exception) {
            Log.e(tag, "Get lesson performance failed: ${e.message}")
            emptyList()
        }
    }

    // Get at-risk students for a class (best_score < 60% on any completed lesson)
    suspend fun getAtRiskStudents(classId: String): List<StudentInfo> {
        return try {
            val students = SupabaseClient.client
                .from("student")
                .select { filter { eq("class_id", classId) } }
                .decodeList<StudentRow>()

            students.filter { student ->
                val progress = SupabaseClient.client
                    .from("student_progress")
                    .select { filter { eq("student_id", student.id) } }
                    .decodeList<ProgressRow>()

                progress.any { p ->
                    p.status == "DONE" && p.quiz_total > 0 &&
                    ((p.best_score ?: p.quiz_score).toFloat() / p.quiz_total) < 0.6f
                }
            }.map { s ->
                StudentInfo(s.id, s.class_id ?: "", s.full_name, s.section, s.grade_level, s.last_active, isAtRisk = true)
            }
        } catch (e: Exception) {
            Log.e(tag, "Get at-risk students failed: ${e.message}")
            emptyList()
        }
    }
}
