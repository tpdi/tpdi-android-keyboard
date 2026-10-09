package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Trim the quiet tail off voice audio before decoding, so the model has no silence to invent text for.
// Off by default: with it off the audio is decoded exactly as before.
val VOICE_INPUT_TRIM_TRAILING_SILENCE = SettingsKey(
    key = booleanPreferencesKey("voice_input_trim_trailing_silence"),
    default = false
)
