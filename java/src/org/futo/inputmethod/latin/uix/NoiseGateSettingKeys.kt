package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Automatic noise gate for voice input: learns the ambient level and turns audio near it down.
// Off by default: with it off the gate isn't created and the audio path is unchanged.
val VOICE_INPUT_NOISE_GATE = SettingsKey(
    key = booleanPreferencesKey("voice_input_noise_gate"),
    default = false
)
