//package com.basahero.elearning.data.local
//
//import android.content.Context
//import com.basahero.elearning.data.local.entity.*
//import kotlinx.serialization.Serializable
//import kotlinx.serialization.json.Json
//
///**
// * DatabaseSeeder
// *
// * Reads grade4_q1.json, grade5_q1.json, grade6_q1.json from assets/data/
// * and inserts all content into Room on the very first app launch.
// *
// * Called once from AppDatabase.Callback.onCreate().
// * Never runs again because the database already exists on subsequent launches.
// */
//class DatabaseSeeder(private val context: Context) {
//
//    private val json = Json {
//        ignoreUnknownKeys = true
//        isLenient         = true
//    }
//
//    suspend fun seed(db: AppDatabase) {
//        // ── Seed grade levels ──────────────────────────────────────────────
//        db.gradeLevelDao().insertAll(
//            listOf(
//                GradeLevelEntity(id = 4, label = "Grade 4"),
//                GradeLevelEntity(id = 5, label = "Grade 5"),
//                GradeLevelEntity(id = 6, label = "Grade 6"),
//            )
//        )
//
//        // ── Seed content from JSON files ───────────────────────────────────
//        // Note: For now, make sure you actually put "grade4_q1.json" in the assets/data folder!
//        val files = listOf("grade4_q1.json", "grade5_q1.json", "grade6_q1.json")
//
//        files.forEach { filename ->
//            try {
//                val raw  = context.assets.open("data/$filename").bufferedReader().readText()
//                val seed = json.decodeFromString<SeedFile>(raw)
//                insertSeedFile(db, seed)
//            } catch (e: Exception) {
//                // If a file is missing (like grade5 or grade6), it will just skip it safely
//                e.printStackTrace()
//            }
//        }
//    }
//
//    private suspend fun insertSeedFile(db: AppDatabase, seed: SeedFile) {
//        // Quarter
//        db.quarterDao().insertAll(listOf(seed.quarter.toEntity()))
//
//        // Lessons + questions + choices
//        seed.lessons.forEach { lesson ->
//            db.lessonDao().insertAll(listOf(lesson.toEntity(seed.quarter.id)))
//
//            val questions = lesson.questions.map { it.toEntity(lesson.id) }
//            db.quizDao().insertQuestions(questions)
//
//            val choices = lesson.questions.flatMap { q ->
//                q.choices.map { it.toEntity(q.id) }
//            }
//            db.quizDao().insertChoices(choices)
//        }
//    }
//}
//
//// ─────────────────────────────────────────────
//// JSON data classes — mirror the shape of each seed file
//// ─────────────────────────────────────────────
//
//@Serializable
//data class SeedFile(
//    val quarter: SeedQuarter,
//    val lessons: List<SeedLesson>
//)
//
//@Serializable
//data class SeedQuarter(
//    val id:            String,
//    val gradeLevelId:  Int,
//    val quarterNumber: Int,
//    val title:         String,
//    val isActive:      Boolean = true   // Q1 defaults to active
//) {
//    fun toEntity() = QuarterEntity(
//        id            = id,
//        gradeLevelId  = gradeLevelId,
//        quarterNumber = quarterNumber,
//        title         = title,
//        isActive      = isActive
//    )
//}
//
//@Serializable
//data class SeedLesson(
//    val id:          String,
//    val orderIndex:  Int,
//    val competency:  String,
//    val title:       String,
//    val passageText: String,
//    val imagePath:   String? = null,
//    val questions:   List<SeedQuestion>
//) {
//    fun toEntity(quarterId: String) = LessonEntity(
//        id          = id,
//        quarterId   = quarterId,
//        orderIndex  = orderIndex,
//        competency  = competency,
//        title       = title,
//        passageText = passageText,
//        imagePath   = imagePath
//    )
//}
//
//@Serializable
//data class SeedQuestion(
//    val id:           String,
//    val questionText: String,
//    val questionType: String,       // MCQ / FILL_IN / SEQUENCING / MATCHING
//    val orderIndex:   Int,
//    val pointsValue:  Int = 1,
//    val choices:      List<SeedChoice>
//) {
//    fun toEntity(lessonId: String) = QuizQuestionEntity(
//        id           = id,
//        lessonId     = lessonId,
//        questionText = questionText,
//        questionType = questionType,
//        orderIndex   = orderIndex,
//        pointsValue  = pointsValue
//    )
//}
//
//@Serializable
//data class SeedChoice(
//    val id:         String,
//    val choiceText: String,
//    val isCorrect:  Boolean,
//    val orderIndex: Int
//) {
//    fun toEntity(questionId: String) = QuizChoiceEntity(
//        id         = id,
//        questionId = questionId,
//        choiceText = choiceText,
//        isCorrect  = isCorrect,
//        orderIndex = orderIndex
//    )
//}
package com.basahero.elearning.data.local

import android.content.Context
import com.basahero.elearning.data.local.entity.*
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json

/**
 * DatabaseSeeder
 *
 * Reads grade4_q1.json, grade5_q1.json, grade6_q1.json from assets/data/
 * and inserts all content into Room on the very first app launch.
 *
 * Called once from AppDatabase.Callback.onCreate().
 * Never runs again because the database already exists on subsequent launches.
 */
class DatabaseSeeder(private val context: Context) {

    private val json = Json {
        ignoreUnknownKeys = true
        isLenient         = true
    }

    suspend fun seed(db: AppDatabase) {
        // ── 1. Seed grade levels ──────────────────────────────────────────────
        db.gradeLevelDao().insertAll(
            listOf(
                GradeLevelEntity(id = 4, label = "Grade 4"),
                GradeLevelEntity(id = 5, label = "Grade 5"),
                GradeLevelEntity(id = 6, label = "Grade 6"),
            )
        )

        // ── 2. Seed a default student for testing ─────────────────────────────
        // This ensures you can always login on a fresh install!
        db.studentDao().insertOrUpdate(
            StudentEntity(
                id         = "student-001",
                classId    = "dev-class-2026",
                fullName   = "Juan Dela Cruz", // <--- Use this to login
                section    = "Mabini",          // <--- Use this to login
                gradeLevel = 4,
                lastActive = System.currentTimeMillis(),
                synced     = false,
                createdAt  = System.currentTimeMillis()
            )
        )

        // ── 3. Seed content from JSON files ───────────────────────────────────
        // Note: For now, make sure you actually put "grade4_q1.json" in the assets/data folder!
        val files = listOf("grade4_q1.json", "grade5_q1.json", "grade6_q1.json")

        files.forEach { filename ->
            try {
                val raw  = context.assets.open("data/$filename").bufferedReader().readText()
                val seed = json.decodeFromString<SeedFile>(raw)
                insertSeedFile(db, seed)
            } catch (e: Exception) {
                // If a file is missing (like grade5 or grade6), it will just skip it safely
                e.printStackTrace()
            }
        }
    }

    private suspend fun insertSeedFile(db: AppDatabase, seed: SeedFile) {
        // Quarter
        db.quarterDao().insertAll(listOf(seed.quarter.toEntity()))

        // Lessons + questions + choices
        seed.lessons.forEach { lesson ->
            db.lessonDao().insertAll(listOf(lesson.toEntity(seed.quarter.id)))

            val questions = lesson.questions.map { it.toEntity(lesson.id) }
            db.quizDao().insertQuestions(questions)

            val choices = lesson.questions.flatMap { q ->
                q.choices.map { it.toEntity(q.id) }
            }
            db.quizDao().insertChoices(choices)
        }
    }
}

// ─────────────────────────────────────────────
// JSON data classes — mirror the shape of each seed file
// ─────────────────────────────────────────────

@Serializable
data class SeedFile(
    val quarter: SeedQuarter,
    val lessons: List<SeedLesson>
)

@Serializable
data class SeedQuarter(
    val id:            String,
    val gradeLevelId:  Int,
    val quarterNumber: Int,
    val title:         String,
    val isActive:      Boolean = true   // Q1 defaults to active
) {
    fun toEntity() = QuarterEntity(
        id            = id,
        gradeLevelId  = gradeLevelId,
        quarterNumber = quarterNumber,
        title         = title,
        isActive      = isActive
    )
}

@Serializable
data class SeedLesson(
    val id:          String,
    val orderIndex:  Int,
    val competency:  String,
    val title:       String,
    val passageText: String,
    val imagePath:   String? = null,
    val questions:   List<SeedQuestion>
) {
    fun toEntity(quarterId: String) = LessonEntity(
        id          = id,
        quarterId   = quarterId,
        orderIndex  = orderIndex,
        competency  = competency,
        title       = title,
        passageText = passageText,
        imagePath   = imagePath
    )
}

@Serializable
data class SeedQuestion(
    val id:           String,
    val questionText: String,
    val questionType: String,       // MCQ / FILL_IN / SEQUENCING / MATCHING
    val orderIndex:   Int,
    val pointsValue:  Int = 1,
    val choices:      List<SeedChoice>
) {
    fun toEntity(lessonId: String) = QuizQuestionEntity(
        id           = id,
        lessonId     = lessonId,
        questionText = questionText,
        questionType = questionType,
        orderIndex   = orderIndex,
        pointsValue  = pointsValue
    )
}

@Serializable
data class SeedChoice(
    val id:         String,
    val choiceText: String,
    val isCorrect:  Boolean,
    val orderIndex: Int
) {
    fun toEntity(questionId: String) = QuizChoiceEntity(
        id         = id,
        questionId = questionId,
        choiceText = choiceText,
        isCorrect  = isCorrect,
        orderIndex = orderIndex
    )
}