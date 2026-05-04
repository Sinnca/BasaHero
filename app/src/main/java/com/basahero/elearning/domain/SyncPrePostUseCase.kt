package com.basahero.elearning.domain

import android.content.Context
import com.basahero.elearning.worker.SyncPrePostWorker

// ─────────────────────────────────────────────────────────────────────────────
// SyncPrePostUseCase
// Called from ViewModel after saving a pre-test or post-test result to Room.
// ─────────────────────────────────────────────────────────────────────────────
class SyncPrePostUseCase {

    fun execute(context: Context) {
        // Enqueue the worker. It will wait for internet, apply jitter, and upload.
        SyncPrePostWorker.enqueue(context)
    }
}
