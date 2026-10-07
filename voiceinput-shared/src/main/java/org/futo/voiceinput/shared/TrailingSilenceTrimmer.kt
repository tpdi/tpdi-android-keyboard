package org.futo.voiceinput.shared

import kotlin.math.sqrt

/**
 * Cuts the quiet tail off 16 kHz audio before it is decoded. Whisper tends to invent text
 * ("Thank you.", "Thanks for watching") for audio that ends in near-silence, and every decode
 * (segment or final) ends in the silence that followed the last word.
 *
 * A 20 ms window counts as quiet when its RMS is under [QUIET_RATIO] of the loudest window, so
 * steady room noise doesn't keep the tail alive. [KEEP_SAMPLES] of the quiet stretch stay, so
 * word endings aren't clipped. The result is never shorter than [MIN_SAMPLES].
 */
object TrailingSilenceTrimmer {
    private const val WINDOW = 320 // 20 ms
    private const val QUIET_RATIO = 0.1f // -20 dB below the loudest window
    private const val KEEP_SAMPLES = 3200 // 200 ms
    private const val MIN_SAMPLES = 3200

    fun trim(samples: FloatArray): FloatArray {
        val windows = samples.size / WINDOW
        if (windows < 2) return samples

        val rms = FloatArray(windows) { w ->
            var sum = 0.0
            for (i in w * WINDOW until (w + 1) * WINDOW) sum += samples[i] * samples[i]
            sqrt(sum / WINDOW).toFloat()
        }
        val loudest = rms.max()
        if (loudest <= 0f) return samples

        var lastLoud = windows - 1
        while (lastLoud > 0 && rms[lastLoud] < loudest * QUIET_RATIO) lastLoud--

        val end = ((lastLoud + 1) * WINDOW + KEEP_SAMPLES).coerceIn(MIN_SAMPLES, samples.size)
        return if (end >= samples.size) samples else samples.copyOf(end)
    }
}
