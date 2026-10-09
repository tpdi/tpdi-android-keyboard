package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// When nothing is selected and the cursor touches a word, Shift selects that word and cycles its
// case (original / lower / Capitalized / UPPER) in place, like it does for a selection.
// Off by default.
val RECAPITALIZE_TOUCHED_WORD = SettingsKey(
    key = booleanPreferencesKey("recapitalize_touched_word"),
    default = false
)
