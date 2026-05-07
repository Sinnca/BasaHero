package com.basahero.elearning.audio

import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import android.util.Log
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import java.util.concurrent.atomic.AtomicBoolean

/**
 * AudioCaptureManager
 *
 * Owns an [AudioRecord] configured for 16kHz mono 16-bit PCM — the exact
 * format Vosk expects.  Raw samples are run through [AudioDsp] before being
 * delivered to any registered [PcmListener].
 *
 * Usage:
 *   val capture = AudioCaptureManager()
 *   capture.start { processedBuffer, length -> vosk.acceptWaveForm(buffer, length) }
 *   // … later …
 *   capture.stop()
 *
 * This class is NOT a singleton — create one per recording session and dispose
 * it with [stop] when done.
 */
class AudioCaptureManager {

    // ── Constants ─────────────────────────────────────────────────────────────
    companion object {
        const val SAMPLE_RATE   = 16_000          // Hz — Vosk's required rate
        const val CHANNEL_CONFIG = AudioFormat.CHANNEL_IN_MONO
        const val AUDIO_FORMAT  = AudioFormat.ENCODING_PCM_16BIT

        // Keep the buffer at ~100 ms of audio so latency stays low.
        // Vosk is happy with any chunk size ≥ ~2 ms.
        private const val BUFFER_DURATION_MS = 100

        private const val TAG = "AudioCaptureManager"
    }

    // ── State ─────────────────────────────────────────────────────────────────
    private val _isCapturing = MutableStateFlow(false)
    val isCapturing: StateFlow<Boolean> = _isCapturing.asStateFlow()

    private val _rmsLevel = MutableStateFlow(0f)
    /** Normalised RMS of the last processed buffer (0.0 – 1.0). */
    val rmsLevel: StateFlow<Float> = _rmsLevel.asStateFlow()

    // ── Internals ─────────────────────────────────────────────────────────────
    private var audioRecord: AudioRecord? = null
    private var captureJob: Job?          = null
    private var pcmListener: PcmListener? = null
    // Guards against double-stop from two threads (IO thread auto-stop + Main thread user stop)
    private val stopped = AtomicBoolean(true)

    /** Callback delivered on the IO dispatcher after DSP processing. */
    fun interface PcmListener {
        fun onPcmAvailable(buffer: ShortArray, length: Int)
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Start capturing.  [onPcm] is called for every ~100 ms chunk of
     * processed audio.  Must hold RECORD_AUDIO permission before calling.
     */
    fun start(onPcm: PcmListener) {
        if (_isCapturing.value) return

        pcmListener = onPcm
        stopped.set(false)
        AudioDsp.resetFilterState()

        val minBuf = AudioRecord.getMinBufferSize(SAMPLE_RATE, CHANNEL_CONFIG, AUDIO_FORMAT)
        val bufSize = maxOf(minBuf, SAMPLE_RATE * BUFFER_DURATION_MS / 1000 * 2) // × 2 for shorts

        val record = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION, // best source for speech
            SAMPLE_RATE,
            CHANNEL_CONFIG,
            AUDIO_FORMAT,
            bufSize
        )

        if (record.state != AudioRecord.STATE_INITIALIZED) {
            Log.e(TAG, "AudioRecord failed to initialise — check permissions")
            record.release()
            return
        }

        audioRecord = record
        record.startRecording()
        _isCapturing.value = true

        captureJob = CoroutineScope(Dispatchers.IO).launch {
            val readBuf = ShortArray(bufSize / 2) // bytes → shorts
            Log.d(TAG, "Capture loop started (bufSize=${readBuf.size} shorts, $SAMPLE_RATE Hz)")

            while (isActive) {
                val read = record.read(readBuf, 0, readBuf.size)
                if (read > 0) {
                    // DSP in-place: noise gate → high-pass → normalize
                    AudioDsp.process(readBuf, read)

                    // Publish RMS for a level-meter UI (normalised 0–1)
                    val rmsNorm = (AudioDsp.computeRms(readBuf, read) / Short.MAX_VALUE)
                        .coerceIn(0f, 1f)
                    _rmsLevel.value = rmsNorm

                    // Deliver to Vosk (or any other consumer)
                    pcmListener?.onPcmAvailable(readBuf, read)
                } else if (read < 0) {
                    Log.e(TAG, "AudioRecord.read error: $read")
                    break
                }
            }

            Log.d(TAG, "Capture loop ended")
        }
    }

    /**
     * Stop capturing and release native resources.
     * Fully idempotent — safe to call from any thread, any number of times.
     */
    fun stop() {
        // Only the first caller gets to do the actual teardown
        if (!stopped.compareAndSet(false, true)) return

        captureJob?.cancel()
        captureJob = null

        audioRecord?.apply {
            try { stop() } catch (_: Exception) {}
            try { release() } catch (_: Exception) {}
        }
        audioRecord = null

        _isCapturing.value = false
        _rmsLevel.value    = 0f
        pcmListener        = null
    }
}
