package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Debug: append every voice result (as returned by the model and as delivered) to a log file. Off by default.
val VOICE_INPUT_LOG_RESULTS = SettingsKey(
    key = booleanPreferencesKey("voice_input_log_results"),
    default = false
)
