package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// With the cursor touching a word, Shift offers that word's Capitalized / UPPER forms in the
// suggestion strip (one more form per tap) instead of changing the text. Off by default.
val CASE_FORM_SUGGESTIONS = SettingsKey(
    key = booleanPreferencesKey("case_form_suggestions"),
    default = false
)
