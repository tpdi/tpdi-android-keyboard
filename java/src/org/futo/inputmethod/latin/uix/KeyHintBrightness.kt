package org.futo.inputmethod.latin.uix

import android.content.Context
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.datastore.preferences.core.intPreferencesKey

// How bright the small hint characters on keys are, as a percentage of the theme's own hint
// opacity. 100 (the default) draws hints exactly as before; 0 hides them; above 100 raises the opacity
// linearly from the theme's own value (at 100) up to fully opaque (at 200).
val KEY_HINT_BRIGHTNESS_PERCENT = SettingsKey(
    key = intPreferencesKey("key_hint_brightness_percent"),
    default = 100
)

object KeyHintBrightness {
    fun apply(context: Context, argb: Int): Int {
        val percent = context.getSetting(KEY_HINT_BRIGHTNESS_PERCENT.key, KEY_HINT_BRIGHTNESS_PERCENT.default)
        if (percent == 100) return argb
        val p = percent.coerceIn(0, 200)
        return Color(argb).let {
            val alpha = if (p <= 100) it.alpha * p / 100f else it.alpha + (1f - it.alpha) * (p - 100) / 100f
            it.copy(alpha = alpha)
        }.toArgb()
    }
}
