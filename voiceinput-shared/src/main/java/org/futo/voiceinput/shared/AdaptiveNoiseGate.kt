package org.futo.voiceinput.shared

import kotlin.math.sqrt

/**
 * Automatic noise gate for 16 kHz mono 16-bit audio. It learns the ambient level (the noise floor)
 * from the audio itself and turns audio that stays close to that floor down, so only the voice
 * reaches the speech model. There is no manual threshold.
 *
 * The floor follows quiet frames down quickly and creeps up slowly, so it adapts when the
 * surroundings get louder. It does not rise (much) while the gate is open, so a long sentence
 * can't raise it. Audio is attenuated rather than zeroed, which leaves the model something
 * natural-sounding at a word onset that was caught late.
 */
class AdaptiveNoiseGate {
    private companion object {
        const val FRAME = 160                // 10 ms
        const val OPEN_RATIO = 3.0f          // open at ~10 dB above the floor
        const val CLOSE_RATIO = 2.0f         // stay open until below ~6 dB above the floor
        const val HOLD_FRAMES = 30           // 300 ms hold so word endings and pauses survive
        const val MIN_FLOOR = 0.0003f
        const val CLOSED_GAIN = 0.1f         // -20 dB
        const val FLOOR_FALL = 0.3f
        const val FLOOR_RISE_CLOSED = 1.003f // per frame
        const val FLOOR_RISE_OPEN = 1.0002f
    }

    private var floor = -1f
    private var open = false
    private var holdLeft = 0
    private var gain = CLOSED_GAIN

    /** Gates [samples] in place. A trailing partial frame reuses the last full frame's decision. */
    fun process(samples: ShortArray, count: Int) {
        var pos = 0
        while (pos < count) {
            val len = minOf(FRAME, count - pos)
            if (len == FRAME) {
                updateState(rms(samples, pos, len))
            }
            val target = if (open) 1f else CLOSED_GAIN
            for (i in 0 until len) {
                val g = gain + (target - gain) * (i + 1) / len
                samples[pos + i] = (samples[pos + i] * g).toInt().toShort()
            }
            gain = target
            pos += len
        }
    }

    private fun rms(s: ShortArray, off: Int, len: Int): Float {
        var sum = 0.0
        for (i in off until off + len) {
            val v = s[i].toFloat() / Short.MAX_VALUE
            sum += v * v
        }
        return sqrt(sum / len).toFloat()
    }

    private fun updateState(level: Float) {
        if (floor < 0f) {
            floor = level.coerceAtLeast(MIN_FLOOR)
            return
        }
        if (level < floor) {
            floor += (level - floor) * FLOOR_FALL
        } else {
            floor *= if (open) FLOOR_RISE_OPEN else FLOOR_RISE_CLOSED
        }
        if (floor < MIN_FLOOR) floor = MIN_FLOOR

        if (!open) {
            if (level > floor * OPEN_RATIO) {
                open = true
                holdLeft = HOLD_FRAMES
            }
        } else if (level > floor * CLOSE_RATIO) {
            holdLeft = HOLD_FRAMES
        } else if (--holdLeft <= 0) {
            open = false
        }
    }
}
