package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Pressing the voice input key while the voice window is open stops and transcribes the recording.
// Off by default: with it off, pressing the key again closes and reopens the window as before.
val VOICE_INPUT_MIC_KEY_TOGGLE = SettingsKey(
    key = booleanPreferencesKey("voice_input_mic_key_toggle"),
    default = false
)
