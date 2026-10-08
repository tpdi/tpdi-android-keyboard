package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Shows Undo and Enter buttons beside the voice input bubble. Off by default: with it off
// nothing extra is drawn.
val VOICE_INPUT_ACTION_BUTTONS = SettingsKey(
    key = booleanPreferencesKey("voice_input_action_buttons"),
    default = false
)
