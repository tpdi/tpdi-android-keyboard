package org.futo.inputmethod.latin.uix

import android.content.Context
import android.net.Uri
import android.provider.OpenableColumns
import java.io.DataInputStream
import java.io.File
import java.io.InputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.time.LocalDate

/**
 * Describes an imported voice input (Whisper ggml) model, so the settings can show what is installed
 * instead of only "Externally imported model". At import the description, the original file name and
 * the date are saved; for models imported before that, the installed file's header is read instead.
 */
object VoiceModelInfo {
    private const val HEADER_INTS = 11  // n_vocab, audio ctx/state/head/layer, text ctx/state/head/layer, n_mels, ftype

    /** Reads the header that follows the 4-byte magic, e.g. "Whisper base, English-only, q8_0". */
    fun describeHeader(afterMagic: InputStream): String? {
        val bytes = ByteArray(HEADER_INTS * 4)
        try {
            DataInputStream(afterMagic).readFully(bytes)
        } catch (e: Exception) {
            return null
        }
        val h = ByteBuffer.wrap(bytes).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer()
        val vocab = h[0]
        val audioState = h[2]
        val audioLayers = h[4]
        val size = when (audioState to audioLayers) {
            384 to 4 -> "tiny"
            512 to 6 -> "base"
            768 to 12 -> "small"
            1024 to 24 -> "medium"
            1280 to 32 -> "large"
            else -> "$audioLayers layers × $audioState"
        }
        val languages = when (vocab) {
            51864 -> "English-only"
            51866 -> "multilingual (v3)"
            else -> "multilingual"
        }
        val type = when (h[10] % 1000) {
            0 -> "f32"
            1 -> "f16"
            2 -> "q4_0"
            3 -> "q4_1"
            7 -> "q8_0"
            8 -> "q5_0"
            9 -> "q5_1"
            else -> "type ${h[10] % 1000}"
        }
        return "Whisper $size, $languages, $type"
    }

    fun describeFile(file: File): String? = try {
        file.inputStream().use { it.skip(4); describeHeader(it) }
    } catch (e: Exception) {
        null
    }

    /** What is saved at import: description, original file name and date. */
    fun importRecord(context: Context, uri: Uri, header: String?): String {
        val fileName = try {
            context.contentResolver.query(uri, arrayOf(OpenableColumns.DISPLAY_NAME), null, null, null)?.use {
                if (it.moveToFirst()) it.getString(0) else null
            }
        } catch (e: Exception) {
            null
        }
        return listOfNotNull(header, fileName, "imported ${LocalDate.now()}").joinToString(" · ")
    }
}
