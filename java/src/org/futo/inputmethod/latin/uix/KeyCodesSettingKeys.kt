package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey


// When on, keys that a hardware keyboard would send as key events (Escape, Tab, Enter where a
// newline is wanted, Home, End, Page Up/Down, Forward Delete, Insert, F1-F12) are sent as real
// KeyEvents instead of as text. When off, behavior is unchanged.
val SEND_KEY_CODES_RATHER_THAN_TEXT = SettingsKey(
    key = booleanPreferencesKey("send_key_codes_rather_than_text"),
    default = false
)
