package org.futo.inputmethod.latin.uix

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import org.futo.inputmethod.keyboard.Key
import org.futo.inputmethod.latin.LatinIME
import org.futo.inputmethod.latin.uix.actions.VoiceInputAction
import org.futo.inputmethod.latin.uix.actions.keyCode

// While the voice input window is open, the keyboard's microphone key is drawn blue, the same blue
// as the microphone on the dictation bar. Off by default: with it off keys are drawn as before.
val VOICE_INPUT_MIC_KEY_BLUE = SettingsKey(
    key = booleanPreferencesKey("voice_input_mic_key_blue"),
    default = false
)

object MicKeyBlueWhenListening {
    private const val BLUE = 0xFF3B82F6.toInt()

    @Volatile
    private var listening = false

    /** Called when the open action window changes; redraws the keys so the microphone updates. */
    fun onWindowChanged(ime: LatinIME, openAction: Action?) {
        val nowListening = openAction === VoiceInputAction
        if (nowListening == listening) return
        listening = nowListening
        ime.latinIMELegacy.mKeyboardSwitcher?.mainKeyboardView?.invalidateAllKeys()
    }

    /** The color to draw [key] in: blue for the microphone key while listening, else [color]. */
    fun tint(context: Context, key: Key, color: Int): Int {
        if (!listening || key.code != VoiceInputAction.keyCode) return color
        return if (context.getSetting(VOICE_INPUT_MIC_KEY_BLUE.key, VOICE_INPUT_MIC_KEY_BLUE.default)) BLUE else color
    }
}
