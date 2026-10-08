package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// While dictating, the keyboard's Undo key undoes the last dictated segment, like the bar's Undo.
// Off by default: with it off the Undo key sends Ctrl+Z to the app as before.
val VOICE_INPUT_UNDO_KEY = SettingsKey(
    key = booleanPreferencesKey("voice_input_undo_key"),
    default = false
)
