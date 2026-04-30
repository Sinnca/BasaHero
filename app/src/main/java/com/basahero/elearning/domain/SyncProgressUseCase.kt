package com.basahero.elearning.domain

import android.content.Context
import com.basahero.elearning.worker.SyncProgressWorker

// ─────────────────────────────────────────────────────────────────────────────
// SyncProgressUseCase
// Called from ViewModel after saving a quiz result to Room.
// ─────────────────────────────────────────────────────────────────────────────
class SyncProgressUseCase {

    fun execute(context: Context) {
        // Enqueue the worker. It will wait for internet, apply jitter, and upload.
        SyncProgressWorker.enqueue(context)

        // Note: When you finish Phase 5 (Speech), uncomment the line below:
        // SyncPronunciationWorker.enqueue(context)
    }
}