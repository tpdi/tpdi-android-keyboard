package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.floatPreferencesKey

// Scales the letters and labels on keys, as a multiplier on top of the theme's own text size.
// 1.0 (the default) draws labels exactly as before.
val KEY_LABEL_SCALE = SettingsKey(
    key = floatPreferencesKey("key_label_scale"),
    default = 1.0f
)
