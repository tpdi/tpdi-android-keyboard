package org.futo.inputmethod.latin.uix

import androidx.datastore.preferences.core.booleanPreferencesKey

// Makes the key_ctrl and key_alt layout keys work: tap one, and the next key is sent as a real
// key event with the modifier held. Off by default; with it off those keys do nothing.
val STICKY_MODIFIER_KEYS = SettingsKey(
    key = booleanPreferencesKey("sticky_modifier_keys"),
    default = false
)
