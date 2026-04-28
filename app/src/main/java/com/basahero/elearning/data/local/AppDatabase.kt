package com.basahero.elearning.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
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
        GameSessionEntity::class,
        GameAnswerEntity::class,
    ],
    version  = 1,
    exportSchema = false // Set to false for now to avoid compilation warnings during dev
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun gradeLevelDao(): GradeLevelDao
    abstract fun quarterDao(): QuarterDao
    abstract fun lessonDao(): LessonDao
    abstract fun quizDao(): QuizDao
    abstract fun studentDao(): StudentDao
    abstract fun progressDao(): ProgressDao
    abstract fun pronunciationDao(): PronunciationDao

    companion object {
        private const val DB_NAME = "readreach.db"

        @Volatile
        private var INSTANCE: AppDatabase? = null

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
                .fallbackToDestructiveMigration() // dev only — replace with Migration in production
                .build()
        }
    }
}