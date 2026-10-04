package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// While dictating over the keyboard, show a button on the listening bar that hides the keyboard
// (leaving only the bar) and brings it back. Has no effect unless "Dictate over the keyboard" is on.
val VOICE_INPUT_HIDE_KEYBOARD_BUTTON = SettingsKey(
    key = booleanPreferencesKey("voice_input_hide_keyboard_button"),
    default = true
)
