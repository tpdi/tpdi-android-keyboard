package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Show the language list without taking window focus from the app. Some apps (Chromium web views,
// e.g. the Substack editor) hide the keyboard the moment their window loses focus.
// Off by default: with it off the list opens exactly as before.
val LANGUAGE_DIALOG_KEEPS_APP_FOCUS = SettingsKey(
    key = booleanPreferencesKey("language_dialog_keeps_app_focus"),
    default = false
)
