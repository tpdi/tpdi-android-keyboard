package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Draws the small hint characters on keys 30% larger, on top of the theme's own hint size.
// Off by default; with it off hints are drawn exactly as before.
val LARGER_KEY_HINTS = SettingsKey(
    key = booleanPreferencesKey("larger_key_hints"),
    default = false
)

const val LARGER_KEY_HINTS_SCALE = 1.3f
