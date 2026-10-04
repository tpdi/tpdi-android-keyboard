package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// While dictating over the keyboard, draw the volume circle over the keys as well as behind the
// bar. Off: only the bar part is drawn and nothing covers the keyboard. Has no effect unless
// "Dictate over the keyboard" is on.
val VOICE_INPUT_CIRCLE_OVER_KEYS = SettingsKey(
    key = booleanPreferencesKey("voice_input_circle_over_keys"),
    default = true
)
