package com.basahero.elearning.audio

import android.util.Log
import com.basahero.elearning.util.VoskManager
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import org.vosk.Recognizer
import java.util.concurrent.atomic.AtomicBoolean

/**
 * SpeechRecognitionEngine
 *
 * Bridges [AudioCaptureManager] (raw DSP-processed PCM) directly into Vosk's
 * [Recognizer], bypassing the Vosk-bundled [SpeechService] so we control every
 * byte that enters the recognizer.
 *
 * Lifecycle:
 *   startListening(onPartial, onResult, onError)
 *   stopListening()   ← call to get the final hypothesis
 *   release()         ← call on composable disposal / ViewModel onCleared
 *
 * The engine is a plain class (not a singleton) — create one per screen that
 * needs speech input and [release] it in DisposableEffect.
 */
class SpeechRecognitionEngine {

    // ── State ─────────────────────────────────────────────────────────────────
    sealed class RecognitionState {
        object Idle      : RecognitionState()
        object Listening : RecognitionState()
        data class Result(val text: String, val isFinal: Boolean) : RecognitionState()
        data class Error(val message: String) : RecognitionState()
    }

    private val _state = MutableStateFlow<RecognitionState>(RecognitionState.Idle)
    val state: StateFlow<RecognitionState> = _state.asStateFlow()

    // ── Internals ─────────────────────────────────────────────────────────────
    private val capture         = AudioCaptureManager()
    private var recognizer      : Recognizer? = null
    private var feedScope       : Job?        = null
    private val resultDelivered = AtomicBoolean(false)
    // True between startListening() and stopListening()/release()
    private val isActive        = AtomicBoolean(false)
    
    // Guards all access to the Vosk native Recognizer pointer
    private val recLock         = Any()

    companion object {
        private const val TAG = "SpeechRecognitionEngine"
    }

    // ── Public API ────────────────────────────────────────────────────────────

    /**
     * Begin recording + recognition.
     *
     * @param targetWords Optional list of words to restrict the vocabulary to (e.g. for pronunciation tests)
     * @param onPartial Called on the main thread with partial (interim) text.
     * @param onResult  Called on the main thread with the final recognised text.
     * @param onError   Called on the main thread if something goes wrong.
     */
    fun startListening(
        targetWords: List<String> = emptyList(),
        onPartial : (String) -> Unit = {},
        onResult  : (String) -> Unit,
        onError   : (String) -> Unit
    ) {
        if (_state.value is RecognitionState.Listening) return

        val model = VoskManager.model
        if (model == null) {
            onError("Speech model is still loading — please wait a moment.")
            return
        }

        try {
            if (targetWords.isNotEmpty()) {
                // Constrain the model to ONLY listen for the target words plus an 'unknown' fallback
                // This eliminates 99% of "invented" hallucinated words from background noise
                val cleanWords = targetWords.map { it.lowercase().replace(Regex("[^a-z]"), "") }
                val grammarJson = "[\"" + cleanWords.joinToString("\", \"") + "\", \"[unk]\"]"
                recognizer = Recognizer(model, AudioCaptureManager.SAMPLE_RATE.toFloat(), grammarJson)
            } else {
                recognizer = Recognizer(model, AudioCaptureManager.SAMPLE_RATE.toFloat())
            }
        } catch (e: Exception) {
            onError("Failed to create recognizer: ${e.message}")
            return
        }

        resultDelivered.set(false)
        isActive.set(true)
        _state.value = RecognitionState.Listening

        capture.start { buffer, length ->
            if (resultDelivered.get()) return@start  // already got a result, drain quietly

            synchronized(recLock) {
                val rec = recognizer ?: return@synchronized
                
                try {
                    if (rec.acceptWaveForm(buffer, length)) {
                        // Full sentence result ready
                        val text = parseVoskJson(rec.result)
                        Log.d(TAG, "Result: $text")
                        if (text.isNotEmpty() && resultDelivered.compareAndSet(false, true)) {
                            // Stop capture immediately so no further chunks are processed
                            capture.stop()
                            _state.value = RecognitionState.Result(text, isFinal = false)
                            CoroutineScope(Dispatchers.Main).launch { onResult(text) }
                        }
                    } else {
                        // Partial result
                        val partial = parseVoskJson(rec.partialResult, key = "partial")
                        if (partial.isNotEmpty()) {
                            CoroutineScope(Dispatchers.Main).launch { onPartial(partial) }
                        }
                    }
                } catch (e: Exception) {
                    Log.e(TAG, "Recognizer error", e)
                }
            }
        }

        Log.d(TAG, "Listening started")
    }

    /**
     * Stop the microphone and flush the final hypothesis.
     * Safe to call even if auto-stop already fired via resultDelivered.
     *
     * @param onFinalResult  Receives the last recognised utterance (may be empty).
     */
    fun stopListening(onFinalResult: (String) -> Unit = {}) {
        // If already stopped (by auto-stop or double-tap), do nothing
        if (!isActive.compareAndSet(true, false)) return

        // capture.stop() is idempotent — safe even if already stopped by resultDelivered path
        capture.stop()

        synchronized(recLock) {
            // Only call finalResult if recognizer is still alive and resultDelivered hasn't fired
            val finalText = if (!resultDelivered.get()) {
                try {
                    parseVoskJson(recognizer?.finalResult)
                } catch (e: Exception) {
                    Log.w(TAG, "finalResult error: ${e.message}")
                    ""
                }
            } else {
                // Result was already delivered via onResult callback; don't call finalResult again
                ""
            }

            Log.d(TAG, "stopListening final=$finalText")
            _state.value = RecognitionState.Result(finalText, isFinal = true)
            CoroutineScope(Dispatchers.Main).launch { onFinalResult(finalText) }

            releaseRecognizer()
        }
    }

    /**
     * Release all native resources.  Safe to call when already released.
     */
    fun release() {
        isActive.set(false)
        capture.stop()
        feedScope?.cancel()
        _state.value = RecognitionState.Idle
        synchronized(recLock) {
            releaseRecognizer()
        }
    }

    /** Expose the real-time RMS level for a level-meter in the UI. */
    val rmsLevel: StateFlow<Float> = capture.rmsLevel

    // ── Helpers ───────────────────────────────────────────────────────────────

    private fun releaseRecognizer() {
        try { recognizer?.close() } catch (_: Exception) {}
        recognizer = null
    }

    /**
     * Safely parse Vosk's JSON hypothesis.
     * {"text": "hello world"} or {"partial": "hel"}
     */
    private fun parseVoskJson(json: String?, key: String = "text"): String {
        if (json.isNullOrBlank()) return ""
        return try {
            JSONObject(json).optString(key, "").trim()
        } catch (_: Exception) {
            ""
        }
    }
}
