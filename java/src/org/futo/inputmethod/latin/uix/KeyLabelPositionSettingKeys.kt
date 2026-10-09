package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.floatPreferencesKey

// Moves the letters on keys from the centre of the key (0.0, the default, as before) toward the
// key's left edge (1.0).
val KEY_LABEL_ANCHOR = SettingsKey(
    key = floatPreferencesKey("key_label_anchor"),
    default = 0.0f
)
