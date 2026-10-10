package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey

// Sound profiles: long-pressing the keyboard's microphone key picks a set of voice-input
// toggles for the room you are in. Off by default: with it off the long press does what it did.
val SOUND_PROFILES = SettingsKey(
    key = booleanPreferencesKey("sound_profiles"),
    default = false
)

/** The profiles the user saved themselves, as JSON: [{"name": "...", "values": {"key": true}}]. */
val SOUND_PROFILES_CUSTOM = SettingsKey(
    key = stringPreferencesKey("sound_profiles_custom"),
    default = "[]"
)

/** Name of the profile picked last, for the check mark in the menu. */
val SOUND_PROFILE_ACTIVE = SettingsKey(
    key = stringPreferencesKey("sound_profile_active"),
    default = ""
)
