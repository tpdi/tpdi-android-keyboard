package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// When dictating over the keyboard: a precise tap on the middle of the volume circle ends the
// session. With it off, only the blue microphone in the suggestion bar stops it. Has no effect
// while "Dictate over the keyboard" is off.
val VOICE_INPUT_TAP_CIRCLE_TO_STOP = SettingsKey(
    key = booleanPreferencesKey("voice_input_tap_circle_to_stop"),
    default = true
)
