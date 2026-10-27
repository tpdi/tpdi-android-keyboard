package org.futo.voiceinput.shared

import android.content.Context
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Debug aid: appends every voice result, as the model returned it and as it was delivered, to
 * voice_results.log in the app's private files folder (adb: run-as <package> cat files/voice_results.log).
 * Tab separated: time, segment or final, seconds of audio, model text, delivered text.
 */
class VoiceResultLog(context: Context) {
    private val file = File(context.filesDir, "voice_results.log")
    private val format = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.US)

    @Synchronized
    fun add(where: String, raw: String, delivered: String, samples: Int) {
        fun flat(s: String) = s.replace('\t', ' ').replace('\n', ' ')
        file.appendText("${format.format(Date())}\t$where\t${"%.1f".format(samples / 16000f)}\t${flat(raw)}\t${flat(delivered)}\n")
    }
}
