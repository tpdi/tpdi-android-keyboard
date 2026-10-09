package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Dictate over the regular keyboard: "Listening..." + Undo in the suggestion bar, the volume
// circle drawn on the keys, and typing allowed during the session. When off, the voice input
// window replaces the keyboard as in the Play Store build (the default).
val VOICE_INPUT_OVER_KEYBOARD = SettingsKey(
    key = booleanPreferencesKey("voice_input_over_keyboard"),
    default = false
)
