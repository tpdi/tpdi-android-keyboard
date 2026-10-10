package org.futo.inputmethod.latin.uix

import android.annotation.SuppressLint
import android.media.AudioFormat
import android.media.AudioRecord
import android.media.MediaRecorder
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlin.math.log10
import kotlin.math.sqrt

/** How loud and how uneven the room sounded while listening. Levels are dBFS. */
data class RoomSound(val medianDb: Float, val spreadDb: Float)

/**
 * Listens to the room for a few seconds and picks the sound profile that fits. The thresholds are
 * first guesses from a few rooms and a TV, not tuned; the measured numbers are shown to the user
 * so they can be corrected.
 */
object SoundCalibration {
    private const val SECONDS = 4
    private const val RATE = 16000
    private const val FRAME = 320            // 20 ms

    private const val QUIET_BELOW_DB = -50f  // median below this: nothing to mask
    private const val UNEVEN_SPREAD_DB = 10f // p90 - p10 above this: speech-like, up and down

    @SuppressLint("MissingPermission")
    suspend fun listen(): RoomSound? = withContext(Dispatchers.IO) {
        val total = RATE * SECONDS
        val buf = ShortArray(total)
        val rec = try {
            AudioRecord(
                MediaRecorder.AudioSource.VOICE_RECOGNITION, RATE,
                AudioFormat.CHANNEL_IN_MONO, AudioFormat.ENCODING_PCM_16BIT, total * 2
            )
        } catch (e: Exception) {
            return@withContext null
        }
        try {
            if (rec.state != AudioRecord.STATE_INITIALIZED) return@withContext null
            rec.startRecording()
            var n = 0
            while (n < total) {
                val r = rec.read(buf, n, total - n)
                if (r <= 0) return@withContext null
                n += r
            }
        } finally {
            rec.release()
        }
        val levels = (0 until total / FRAME).map { f ->
            var s = 0.0
            for (i in f * FRAME until (f + 1) * FRAME) {
                val v = buf[i].toDouble() / Short.MAX_VALUE
                s += v * v
            }
            (20 * log10(sqrt(s / FRAME) + 1e-6)).toFloat()
        }.sorted()
        RoomSound(levels[levels.size / 2], levels[levels.size * 9 / 10] - levels[levels.size / 10])
    }

    /** Name of the built-in profile that fits [room]. */
    fun choose(room: RoomSound): String = when {
        room.medianDb < QUIET_BELOW_DB -> "Quiet room"
        room.spreadDb >= UNEVEN_SPREAD_DB -> "TV on"
        else -> "Outside / noisy"
    }
}
