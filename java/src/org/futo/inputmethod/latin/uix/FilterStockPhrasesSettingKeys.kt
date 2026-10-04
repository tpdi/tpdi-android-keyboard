package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Drops a voice result that is exactly a stock subtitle phrase such as "Thanks for watching".
// Off by default: it can in principle drop real speech.
val VOICE_INPUT_FILTER_STOCK_PHRASES = SettingsKey(
    key = booleanPreferencesKey("voice_input_filter_stock_phrases"),
    default = false
)
