package com.basahero.elearning.audio

/**
 * AudioDsp — DSP preprocessing pipeline for 16kHz PCM shorts.
 *
 * Pipeline (applied in order):
 *   1. Noise Gate     — zero out samples below an energy threshold
 *   2. High-Pass Filter — remove DC offset and low-frequency rumble (1st-order IIR)
 *   3. Normalization  — scale the entire buffer so peak = ~0.9 * Short.MAX_VALUE
 *
 * All operations are in-place on a ShortArray to avoid allocations on the audio thread.
 */
object AudioDsp {

    // ── Noise Gate ────────────────────────────────────────────────────────────
    // Samples whose RMS energy (over the whole buffer) falls below this fraction
    // of max amplitude are considered silence and zeroed out.
    // 0.03f (3%) effectively kills most room noise and computer fan hums
    private const val NOISE_GATE_RMS_THRESHOLD = 0.03f

    // ── High-Pass Filter ─────────────────────────────────────────────────────
    // 1st-order IIR high-pass: y[n] = α * (y[n-1] + x[n] - x[n-1])
    // Cutoff ≈ fs * (1 - α) / (2π).  At 16kHz, α=0.97 → cutoff ≈ 155 Hz.
    private const val HP_ALPHA = 0.97f

    // ── Normalization ─────────────────────────────────────────────────────────
    // After filtering, scale so the peak sample reaches this fraction of max.
    private const val NORM_TARGET = 0.9f

    // ── Persistent filter state ───────────────────────────────────────────────
    // These survive across calls so the filter is continuous between buffers.
    @Volatile private var hpPrevIn  = 0f
    @Volatile private var hpPrevOut = 0f

    /**
     * Reset the high-pass filter state (call when starting a new recording session).
     */
    fun resetFilterState() {
        hpPrevIn  = 0f
        hpPrevOut = 0f
    }

    /**
     * Apply the full DSP chain to [buffer] (in-place).
     *
     * @param buffer   Raw 16-bit PCM samples captured at 16 kHz.
     * @param length   Number of valid samples in [buffer] (may be < buffer.size).
     * @return         The same [buffer] reference (for chaining convenience).
     */
    fun process(buffer: ShortArray, length: Int): ShortArray {
        if (length <= 0) return buffer

        // ── Step 1: Noise Gate ────────────────────────────────────────────────
        val rms = computeRms(buffer, length)
        val gateThreshold = NOISE_GATE_RMS_THRESHOLD * Short.MAX_VALUE
        if (rms < gateThreshold) {
            // Buffer is below noise floor — zero it out entirely
            buffer.fill(0, 0, length)
            return buffer
        }

        // ── Step 2: High-Pass Filter (remove DC + low-freq rumble) ───────────
        var prevIn  = hpPrevIn
        var prevOut = hpPrevOut
        for (i in 0 until length) {
            val x = buffer[i].toFloat()
            val y = HP_ALPHA * (prevOut + x - prevIn)
            prevIn  = x
            prevOut = y
            buffer[i] = y.coerceIn(-32768f, 32767f).toInt().toShort()
        }
        hpPrevIn  = prevIn
        hpPrevOut = prevOut

        // ── Step 3: Normalization ─────────────────────────────────────────────
        val peak = findPeak(buffer, length)
        if (peak > 0f) {
            val gain = (NORM_TARGET * Short.MAX_VALUE) / peak
            // Only apply gain if it would actually change something meaningful
            // and avoid clipping (cap gain at 8× to prevent over-amplifying silence)
            val clampedGain = gain.coerceAtMost(8f)
            if (clampedGain != 1f) {
                for (i in 0 until length) {
                    buffer[i] = (buffer[i] * clampedGain)
                        .coerceIn(-32768f, 32767f)
                        .toInt()
                        .toShort()
                }
            }
        }

        return buffer
    }

    /**
     * Compute the Root Mean Square amplitude of [buffer][0..[length]).
     */
    fun computeRms(buffer: ShortArray, length: Int): Float {
        if (length <= 0) return 0f
        var sum = 0.0
        for (i in 0 until length) {
            val s = buffer[i].toDouble()
            sum += s * s
        }
        return Math.sqrt(sum / length).toFloat()
    }

    private fun findPeak(buffer: ShortArray, length: Int): Float {
        var peak = 0f
        for (i in 0 until length) {
            val abs = Math.abs(buffer[i].toFloat())
            if (abs > peak) peak = abs
        }
        return peak
    }
}
