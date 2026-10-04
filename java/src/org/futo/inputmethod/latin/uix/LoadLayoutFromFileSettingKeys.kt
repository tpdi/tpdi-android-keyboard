package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Shows a "Load from file" button in the custom layout editor. Off by default.
val SHOW_LOAD_LAYOUT_FROM_FILE = SettingsKey(
    key = booleanPreferencesKey("show_load_layout_from_file"),
    default = false
)
