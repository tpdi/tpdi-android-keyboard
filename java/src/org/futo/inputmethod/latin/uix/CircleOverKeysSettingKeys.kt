package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// While dictating over the keyboard, don't draw the volume circle over the keys: only the bar part
// is drawn and nothing covers the keyboard. Off by default.
// Has no effect unless "Dictate over the keyboard" is on.
val VOICE_INPUT_NO_CIRCLE_OVER_KEYS = SettingsKey(
    key = booleanPreferencesKey("voice_input_no_circle_over_keys"),
    default = false
)
