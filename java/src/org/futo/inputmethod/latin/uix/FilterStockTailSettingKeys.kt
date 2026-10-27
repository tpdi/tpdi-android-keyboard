package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Cuts stock subtitle phrases such as "Thanks for subscribing" off the end of a longer voice result.
// Off by default: it can in principle cut real speech that ends with those words.
val VOICE_INPUT_FILTER_STOCK_TAIL = SettingsKey(
    key = booleanPreferencesKey("voice_input_filter_stock_tail"),
    default = false
)
