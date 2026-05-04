package com.basahero.elearning.worker

import android.content.Context
import android.util.Log
import androidx.work.*
import com.basahero.elearning.data.local.AppDatabase
import com.basahero.elearning.data.local.DatabaseSeeder
import com.basahero.elearning.data.remote.SupabaseClient
import io.github.jan.supabase.postgrest.from
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import java.util.concurrent.TimeUnit
import kotlin.random.Random

// ─────────────────────────────────────────────────────────────────────────────
// SyncProgressWorker
// Uploads all unsynced STUDENT_PROGRESS rows to Supabase.
// ─────────────────────────────────────────────────────────────────────────────
class SyncProgressWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = "SyncProgressWorker"
    private val db = AppDatabase.getInstance(context, DatabaseSeeder(context))

    companion object {
        const val WORK_NAME = "sync_progress"

        @Serializable
        data class ProgressRow(
            val id: String,
            val student_id: String,
            val lesson_id: String,
            val status: String,
            val quiz_score: Int,
            val quiz_total: Int,
            // PHASE 3B NEW COLUMNS
            val first_score: Int?,
            val best_score: Int,
            val attempt_count: Int,
            val completed_at: String?,
            val synced_at: String
        )

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val jitterSeconds = Random.nextLong(0L, 30L)

            val request = OneTimeWorkRequestBuilder<SyncProgressWorker>()
                .setConstraints(constraints)
                .setInitialDelay(jitterSeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(tag, "Starting progress sync...")

        return@withContext try {
            val unsyncedRows = db.progressDao().getUnsyncedProgress()

            if (unsyncedRows.isEmpty()) {
                Log.d(tag, "Nothing to sync — all progress already uploaded.")
                return@withContext Result.success()
            }

            // Filter out progress for dummy students (synced = false)
            val validRows = unsyncedRows.filter { row ->
                db.studentDao().getById(row.studentId)?.synced == true
            }

            if (validRows.isEmpty()) {
                Log.d(tag, "No valid progress to sync (only local dummy data). Marking as synced to stop retries.")
                db.progressDao().markSyncedBatch(unsyncedRows.map { it.id })
                return@withContext Result.success()
            }

            Log.d(tag, "Uploading ${validRows.size} progress records...")

            val chunks = validRows.chunked(50)
            var successCount = 0
            var failCount = 0

            chunks.forEach { chunk ->
                try {
                    val rows = chunk.map { entity ->
                        ProgressRow(
                            id = entity.id,
                            student_id = entity.studentId,
                            lesson_id = entity.lessonId,
                            status = entity.status,
                            quiz_score = entity.quizScore,
                            quiz_total = entity.quizTotal,
                            // Added Phase 3B mappings
                            first_score = entity.firstScore,
                            best_score = entity.bestScore,
                            attempt_count = entity.attemptCount,
                            completed_at = entity.completedAt?.let { java.time.Instant.ofEpochMilli(it).toString() },
                            synced_at = java.time.Instant.now().toString()
                        )
                    }

                    SupabaseClient.client
                        .from("student_progress")
                        .upsert(rows) {
                            onConflict = "id"
                        }

                    val chunkIds = chunk.map { it.id }
                    db.progressDao().markSyncedBatch(chunkIds)

                    successCount += chunk.size
                    Log.d(tag, "✓ Chunk uploaded: ${chunk.size} records")

                } catch (e: Exception) {
                    failCount += chunk.size
                    Log.e(tag, "✗ Chunk upload failed: ${e.message}")
                }
            }

            if (failCount > 0 && successCount == 0) {
                Result.retry()
            } else {
                Result.success()
            }

        } catch (e: Exception) {
            Log.e(tag, "Sync failed entirely: ${e.message}")
            Result.retry()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SyncPrePostWorker (NEW FOR PHASE 3B)
// Uploads all unsynced PRE_POST_TEST rows to Supabase.
// ─────────────────────────────────────────────────────────────────────────────
class SyncPrePostWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = "SyncPrePostWorker"
    private val db = AppDatabase.getInstance(context, DatabaseSeeder(context))

    companion object {
        const val WORK_NAME = "sync_prepost"

        @Serializable
        data class PrePostRow(
            val id: String,
            val student_id: String,
            val quarter_id: String,
            val test_type: String,
            val score: Int,
            val total: Int,
            val completed_at: String,
            val synced_at: String
        )

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val jitterSeconds = Random.nextLong(0L, 30L)

            val request = OneTimeWorkRequestBuilder<SyncPrePostWorker>()
                .setConstraints(constraints)
                .setInitialDelay(jitterSeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.KEEP,
                request
            )
        }
    }

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        return@withContext try {
            val unsyncedRows = db.prePostDao().getUnsyncedResults()
            if (unsyncedRows.isEmpty()) return@withContext Result.success()

            // Filter out dummy students
            val validRows = unsyncedRows.filter { row ->
                db.studentDao().getById(row.studentId)?.synced == true
            }

            if (validRows.isEmpty()) {
                Log.d(tag, "No valid pre-post to sync (only local dummy data). Marking as synced.")
                unsyncedRows.forEach { db.prePostDao().markSynced(it.id) }
                return@withContext Result.success()
            }

            val rows = validRows.map { entity ->
                PrePostRow(
                    id = entity.id,
                    student_id = entity.studentId,
                    quarter_id = entity.quarterId,
                    test_type = entity.testType,
                    score = entity.score,
                    total = entity.total,
                    completed_at = java.time.Instant.ofEpochMilli(entity.completedAt).toString(),
                    synced_at = java.time.Instant.now().toString()
                )
            }

            SupabaseClient.client.from("pre_post_test").upsert(rows) { onConflict = "id" }

            unsyncedRows.forEach { db.prePostDao().markSynced(it.id) }
            Result.success()
        } catch (e: Exception) {
            Log.e(tag, "PrePost Sync failed: ${e.message}")
            Result.retry()
        }
    }
}

// ─────────────────────────────────────────────────────────────────────────────
// SyncStudentWorker
// Pulls NEW students added by teacher from Supabase down to Room.
// ─────────────────────────────────────────────────────────────────────────────
class SyncStudentWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    private val tag = "SyncStudentWorker"
    private val db = AppDatabase.getInstance(context, DatabaseSeeder(context))

    companion object {
        const val WORK_NAME = "sync_students"

        fun enqueue(context: Context) {
            val constraints = Constraints.Builder()
                .setRequiredNetworkType(NetworkType.CONNECTED)
                .build()

            val jitterSeconds = Random.nextLong(0L, 15L)

            val request = OneTimeWorkRequestBuilder<SyncStudentWorker>()
                .setConstraints(constraints)
                .setInitialDelay(jitterSeconds, TimeUnit.SECONDS)
                .setBackoffCriteria(
                    BackoffPolicy.EXPONENTIAL,
                    WorkRequest.MIN_BACKOFF_MILLIS,
                    TimeUnit.MILLISECONDS
                )
                .addTag(WORK_NAME)
                .build()

            WorkManager.getInstance(context).enqueueUniqueWork(
                WORK_NAME,
                ExistingWorkPolicy.REPLACE,
                request
            )
        }
    }

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

    override suspend fun doWork(): Result = withContext(Dispatchers.IO) {
        Log.d(tag, "Syncing student roster from Supabase...")

        return@withContext try {
            val remoteStudents = SupabaseClient.client
                .from("student")
                .select()
                .decodeList<StudentRow>()

            remoteStudents.forEach { row ->
                val entity = com.basahero.elearning.data.local.entity.StudentEntity(
                    id = row.id,
                    classId = row.class_id ?: "",
                    fullName = row.full_name,
                    section = row.section,
                    gradeLevel = row.grade_level,
                    lastActive = row.last_active?.let { java.time.Instant.parse(it).toEpochMilli() },
                    synced = true,
                    createdAt = row.created_at?.let { java.time.Instant.parse(it).toEpochMilli() } ?: System.currentTimeMillis()
                )
                db.studentDao().insertOrUpdate(entity)
            }

            Log.d(tag, "✓ Student roster sync complete")
            Result.success()

        } catch (e: Exception) {
            Log.e(tag, "Student sync failed: ${e.message}")
            Result.retry()
        }
    }
}