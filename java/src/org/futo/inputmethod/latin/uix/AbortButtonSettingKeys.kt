package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Show an Abort button in the voice input window and on the dictation bar: it stops the recording and
// drops any transcription still running, like dismissing the keyboard but leaving it up. Off by default.
val VOICE_INPUT_ABORT_BUTTON = SettingsKey(
    key = booleanPreferencesKey("voice_input_abort_button"),
    default = false
)
