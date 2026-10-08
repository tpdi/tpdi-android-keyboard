package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Show the text of a quick-copy chip even when the source app flagged the copy as sensitive.
// Off by default: flagged copies keep showing dots, as before.
val QUICK_CLIP_SHOW_SENSITIVE = SettingsKey(
    key = booleanPreferencesKey("quick_clip_show_sensitive"),
    default = false
)
