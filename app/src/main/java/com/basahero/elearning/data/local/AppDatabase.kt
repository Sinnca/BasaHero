package com.basahero.elearning.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import com.basahero.elearning.data.local.dao.*
import com.basahero.elearning.data.local.entity.*
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        GradeLevelEntity::class,
        QuarterEntity::class,
        LessonEntity::class,
        QuizQuestionEntity::class,
        QuizChoiceEntity::class,
        StudentEntity::class,
        StudentProgressEntity::class,
        PronunciationAttemptEntity::class,
        // Removed the Game entities for now since they belong to Phase 6
        PrePostQuestionEntity::class,
        PrePostChoiceEntity::class,
        PrePostTestEntity::class,
    ],
    version  = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gradeLevelDao(): GradeLevelDao
    abstract fun quarterDao(): QuarterDao
    abstract fun lessonDao(): LessonDao
    abstract fun quizDao(): QuizDao
    abstract fun studentDao(): StudentDao
    abstract fun progressDao(): ProgressDao
    abstract fun pronunciationDao(): PronunciationDao
    abstract fun prePostDao(): PrePostDao // NEW DAO

    companion object {
        private const val DB_NAME = "readreach.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

        // The safe migration rules moving from v1 to v2
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE lesson ADD COLUMN highlighted_words TEXT NOT NULL DEFAULT '[]'")

                // first_score is nullable
                db.execSQL("ALTER TABLE student_progress ADD COLUMN first_score INTEGER")
                db.execSQL("ALTER TABLE student_progress ADD COLUMN best_score INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE student_progress ADD COLUMN attempt_count INTEGER NOT NULL DEFAULT 1")

                // Recreate pronunciation_attempt without synced column
                db.execSQL("""CREATE TABLE pronunciation_attempt_new (
                    id TEXT NOT NULL PRIMARY KEY, student_id TEXT NOT NULL,
                    lesson_id TEXT NOT NULL, attempt_number INTEGER NOT NULL,
                    score INTEGER NOT NULL, is_best INTEGER NOT NULL DEFAULT 0,
                    transcript TEXT NOT NULL, feedback_json TEXT NOT NULL,
                    attempted_at INTEGER NOT NULL,
                    FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE,
                    FOREIGN KEY(lesson_id) REFERENCES lesson(id) ON DELETE CASCADE)""")
                db.execSQL("""INSERT INTO pronunciation_attempt_new
                    SELECT id,student_id,lesson_id,attempt_number,score,
                    is_best,transcript,feedback_json,attempted_at FROM pronunciation_attempt""")
                db.execSQL("DROP TABLE pronunciation_attempt")
                db.execSQL("ALTER TABLE pronunciation_attempt_new RENAME TO pronunciation_attempt")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pa_student ON pronunciation_attempt(student_id)")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_pa_lesson ON pronunciation_attempt(lesson_id)")

                // Create Pre/Post Tables
                db.execSQL("""CREATE TABLE IF NOT EXISTS pre_post_question (
                    id TEXT NOT NULL PRIMARY KEY, quarter_id TEXT NOT NULL,
                    test_type TEXT NOT NULL, question_text TEXT NOT NULL,
                    question_type TEXT NOT NULL, order_index INTEGER NOT NULL,
                    points_value INTEGER NOT NULL DEFAULT 1,
                    FOREIGN KEY(quarter_id) REFERENCES quarter(id) ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_ppq_quarter ON pre_post_question(quarter_id)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS pre_post_choice (
                    id TEXT NOT NULL PRIMARY KEY, question_id TEXT NOT NULL,
                    choice_text TEXT NOT NULL, is_correct INTEGER NOT NULL,
                    order_index INTEGER NOT NULL,
                    FOREIGN KEY(question_id) REFERENCES pre_post_question(id) ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_ppc_question ON pre_post_choice(question_id)")

                db.execSQL("""CREATE TABLE IF NOT EXISTS pre_post_test (
                    id TEXT NOT NULL PRIMARY KEY, student_id TEXT NOT NULL,
                    quarter_id TEXT NOT NULL, test_type TEXT NOT NULL,
                    score INTEGER NOT NULL, total INTEGER NOT NULL,
                    completed_at INTEGER NOT NULL, synced INTEGER NOT NULL DEFAULT 0,
                    FOREIGN KEY(student_id) REFERENCES student(id) ON DELETE CASCADE)""")
                db.execSQL("CREATE INDEX IF NOT EXISTS idx_ppt_student ON pre_post_test(student_id)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS idx_ppt_unique ON pre_post_test(student_id,quarter_id,test_type)")
            }
        }

        fun getInstance(context: Context, seeder: DatabaseSeeder): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                INSTANCE ?: buildDatabase(context, seeder).also { INSTANCE = it }
            }
        }

        private fun buildDatabase(
            context: Context,
            seeder:  DatabaseSeeder
        ): AppDatabase {
            return Room.databaseBuilder(
                context.applicationContext,
                AppDatabase::class.java,
                DB_NAME
            )
                .addMigrations(MIGRATION_1_2) // Applies the schema update safely
                .addCallback(object : Callback() {
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        super.onCreate(db)
                        // Seed content on first install — runs only once
                        INSTANCE?.let { database ->
                            CoroutineScope(Dispatchers.IO).launch {
                                seeder.seed(database)
                            }
                        }
                    }
                })
                .fallbackToDestructiveMigration() // Safely clears db if a migration ever fails
                .build()
        }
    }
}