package org.futo.inputmethod.latin.uix.settings.pages

import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.stringResource
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.KEY_HINT_BRIGHTNESS_PERCENT
import org.futo.inputmethod.latin.uix.AndroidTextInput
import org.futo.inputmethod.latin.uix.KEY_HINT_SCALE_PERCENT
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.ScrollableList
import org.futo.inputmethod.latin.uix.settings.SettingSlider
import kotlin.math.roundToInt

// Key hint size and brightness, with the real keyboard shown below so changes are visible as you drag.
@Composable
fun KeyHintsScreen() {
    ScrollableList(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenTitle(stringResource(R.string.key_hints_title), showBack = true)

        // The real keyboard shows below, as on the Resize page, so changes are visible as you drag.
        AndroidTextInput(allowPredictions = false)

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
