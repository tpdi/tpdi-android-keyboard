package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Scales each single-character key hint so its glyph fills about the same box. Off by default.
val NORMALIZE_HINT_GLYPH_SIZE = SettingsKey(
    key = booleanPreferencesKey("normalize_hint_glyph_size"),
    default = false
)
