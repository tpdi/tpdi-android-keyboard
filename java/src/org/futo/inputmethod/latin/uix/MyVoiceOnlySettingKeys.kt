package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Prototype: stereo capture plus a gate that favours sound louder on the second microphone.
// Off by default: with it off the recorder is the stock mono one and the audio path is unchanged.
val VOICE_INPUT_MY_VOICE_ONLY = SettingsKey(
    key = booleanPreferencesKey("voice_input_my_voice_only"),
    default = false
)
