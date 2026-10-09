package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Lets dragging on the space bar move the cursor up and down as well as left and right: each step
// up or down sends an Up or Down key event, like the arrow keys. Off by default.
val SPACE_SWIPE_VERTICAL = SettingsKey(
    key = booleanPreferencesKey("space_swipe_vertical"),
    default = false
)
