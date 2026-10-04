package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Drops text the speech model made up (repeated words or sentences, more words than the speech
// could hold, a decode of trailing silence). Off by default, as in the Play Store build.
val VOICE_INPUT_FILTER_MADE_UP_TEXT = SettingsKey(
    key = booleanPreferencesKey("voice_input_filter_made_up_text"),
    default = false
)
