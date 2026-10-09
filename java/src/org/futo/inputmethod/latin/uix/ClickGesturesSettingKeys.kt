package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Detects two or more sharp clicks in the microphone audio and treats them as Enter. Off by
// default: with it off the click detector doesn't run at all.
val VOICE_INPUT_CLICK_GESTURES = SettingsKey(
    key = booleanPreferencesKey("voice_input_click_gestures"),
    default = false
)
