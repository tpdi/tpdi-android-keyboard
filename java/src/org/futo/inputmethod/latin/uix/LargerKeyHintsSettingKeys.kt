package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.intPreferencesKey

// Scales the small hint characters on keys, as a percentage on top of the theme's own hint size.
// 100 (the default) draws hints exactly as before.
val KEY_HINT_SCALE_PERCENT = SettingsKey(
    key = intPreferencesKey("key_hint_scale_percent"),
    default = 100
)
