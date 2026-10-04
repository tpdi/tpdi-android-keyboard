package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Buttons to switch, while dictating, between dictating over the keyboard and the full voice
// input window, without ending the session. "Dictate over the keyboard" decides which one a
// session starts in.
val VOICE_INPUT_SWITCH_MODE_BUTTONS = SettingsKey(
    key = booleanPreferencesKey("voice_input_switch_mode_buttons"),
    default = true
)
