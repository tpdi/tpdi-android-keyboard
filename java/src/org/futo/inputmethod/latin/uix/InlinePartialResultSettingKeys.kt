package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Shows the words recognized so far inside the voice input bubble while still listening.
// Off by default, as in the Play Store build.
val VOICE_INPUT_INLINE_PARTIAL_RESULT = SettingsKey(
    key = booleanPreferencesKey("voice_input_inline_partial_result"),
    default = false
)
