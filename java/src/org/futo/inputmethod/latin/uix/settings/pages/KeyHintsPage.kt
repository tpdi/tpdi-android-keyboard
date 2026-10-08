package org.futo.inputmethod.latin.uix.settings.pages

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.RichInputMethodManager
import org.futo.inputmethod.latin.uix.KEY_HINT_BRIGHTNESS_PERCENT
import org.futo.inputmethod.latin.uix.KEY_HINT_SCALE_PERCENT
import org.futo.inputmethod.latin.uix.KeyboardLayoutPreview
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.ScrollableList
import org.futo.inputmethod.latin.uix.settings.SettingSlider
import org.futo.inputmethod.latin.uix.settings.useDataStoreValue
import kotlin.math.roundToInt

// Key hint size and brightness, with a keyboard preview above the sliders that redraws on every change.
@Composable
fun KeyHintsScreen() {
    val width = (LocalConfiguration.current.screenWidthDp - 32).coerceAtLeast(64).coerceAtMost(500)
    val layout = remember {
        try { RichInputMethodManager.getInstance().currentSubtype.keyboardLayoutSetName } catch (e: Exception) { "qwerty" }
    }

    val size = useDataStoreValue(KEY_HINT_SCALE_PERCENT)
    val brightness = useDataStoreValue(KEY_HINT_BRIGHTNESS_PERCENT)

    ScrollableList(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenTitle(stringResource(R.string.key_hints_title), showBack = true)

        Spacer(Modifier.height(8.dp))
        KeyboardLayoutPreview(id = layout, width = width.dp, redrawKey = size to brightness)
        Spacer(Modifier.height(16.dp))

        SettingSlider(
            title = stringResource(R.string.keyboard_settings_key_hint_size),
            subtitle = stringResource(R.string.keyboard_settings_key_hint_size_subtitle),
            setting = KEY_HINT_SCALE_PERCENT,
            range = 0.0f .. 200.0f,
            transform = { (it / 5f).roundToInt() * 5 },
            indicator = { "$it%" }
        )
        SettingSlider(
            title = stringResource(R.string.keyboard_settings_key_hint_brightness),
            subtitle = stringResource(R.string.keyboard_settings_key_hint_brightness_subtitle),
            setting = KEY_HINT_BRIGHTNESS_PERCENT,
            range = 0.0f .. 200.0f,
            transform = { (it / 5f).roundToInt() * 5 },
            indicator = { "$it%" }
        )
    }
}
