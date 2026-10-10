package org.futo.voiceinput.shared

import android.media.AudioRecord
import kotlin.math.log10
import kotlin.math.sqrt

/**
 * Prototype "my voice only" gate. Reads 16 kHz stereo from [AudioRecord], mixes it down to mono
 * and turns down everything except sound that is both louder than the learned background and
 * louder on the second microphone than on the first, which is what the user's own voice looks
 * like when the phone is held in the hand, and within about 12 dB of the recent peak, so the
 * quieter TV is shut out between phrases. A TV across the room reaches both microphones about
 * equally.
 *
 * The thresholds were measured on one phone (Galaxy S23 Ultra, held in the hand, TV in the same
 * room): own voice was about 6 dB louder on channel 1, TV-only about 2 dB. Other phones, mic
 * layouts or grips will need different numbers. It does not recognise a voice, only where the
 * sound comes from and how loud it is.
 */
class MyVoiceOnlyGate {
    private companion object {
        const val FRAME = 160                  // 10 ms at 16 kHz
        const val OPEN_RATIO = 3.0f            // ~10 dB above the learned floor
        const val CLOSE_RATIO = 2.0f
        const val MIN_FLOOR = 0.0003f
        const val FLOOR_FALL = 0.3f
        const val FLOOR_RISE = 1.003f
        const val HOLD_FRAMES = 30             // 300 ms
        const val CLOSED_GAIN = 0.05f          // -26 dB
        const val DIFF_OPEN_DB = -3.5f         // channel 0 minus channel 1, smoothed
        const val DIFF_SMOOTH = 0.2f
        const val PEAK_DECAY = 0.99885f         // ~1 dB per second
        const val PEAK_RATIO = 0.25f            // must be within ~12 dB of the recent peak
    }

    private var raw = ShortArray(0)
    private var floor = -1f
    private var open = false
    private var holdLeft = 0
    private var gain = CLOSED_GAIN
    private var diffDb = 0f
    private var peak = 0f

    /** Same contract as [AudioRecord.read] into [out] (mono): returns mono samples read. */
    fun read(recorder: AudioRecord, out: ShortArray, count: Int, mode: Int): Int {
        if (raw.size < count * 2) raw = ShortArray(count * 2)
        val n = recorder.read(raw, 0, count * 2, mode)
        if (n <= 0) return n
        val frames = n / 2
        var pos = 0
        while (pos < frames) {
            val len = minOf(FRAME, frames - pos)
            if (len == FRAME) update(pos)
            val target = if (open) 1f else CLOSED_GAIN
            for (i in 0 until len) {
                val g = gain + (target - gain) * (i + 1) / len
                val l = raw[(pos + i) * 2].toInt()
                val r = raw[(pos + i) * 2 + 1].toInt()
                out[pos + i] = (((l + r) / 2) * g).toInt().toShort()
            }
            gain = target
            pos += len
        }
        return frames
    }

    private fun update(pos: Int) {
        var l2 = 0.0
        var r2 = 0.0
        for (i in 0 until FRAME) {
            val l = raw[(pos + i) * 2].toFloat() / Short.MAX_VALUE
            val r = raw[(pos + i) * 2 + 1].toFloat() / Short.MAX_VALUE
            l2 += l * l
            r2 += r * r
        }
        val level = sqrt((l2 + r2) / (2 * FRAME)).toFloat()
        if (l2 > 1e-9 && r2 > 1e-9) {
            val d = (10 * log10(l2 / r2)).toFloat()
            diffDb += (d - diffDb) * DIFF_SMOOTH
        }

        peak = maxOf(level, peak * PEAK_DECAY)
        if (floor < 0f) {
            floor = level.coerceAtLeast(MIN_FLOOR)
            return
        }
        if (level < floor) floor += (level - floor) * FLOOR_FALL else if (!open) floor *= FLOOR_RISE
        if (floor < MIN_FLOOR) floor = MIN_FLOOR

        val fromUser = diffDb < DIFF_OPEN_DB && level > peak * PEAK_RATIO
        if (!open) {
            if (level > floor * OPEN_RATIO && fromUser) {
                open = true
                holdLeft = HOLD_FRAMES
            }
        } else if (level > floor * CLOSE_RATIO && fromUser) {
            holdLeft = HOLD_FRAMES
        } else if (--holdLeft <= 0) {
            open = false
        }
    }
}
