package org.futo.inputmethod.latin.uix.settings.pages

import android.content.Context
import android.view.inputmethod.EditorInfo
import android.view.inputmethod.InputMethodManager
import android.widget.EditText
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import kotlinx.coroutines.delay
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.KEY_HINT_BRIGHTNESS_PERCENT
import org.futo.inputmethod.latin.uix.KEY_HINT_SCALE
import org.futo.inputmethod.latin.uix.settings.ScreenTitle
import org.futo.inputmethod.latin.uix.settings.ScrollableList
import org.futo.inputmethod.latin.uix.settings.SettingSlider
import kotlin.math.roundToInt

// Key hint size and brightness, with the real keyboard shown below so changes are visible as you drag.
// A nearly invisible, focused text field is what makes the keyboard appear.
@Composable
fun KeyHintsScreen() {
    val context = LocalContext.current
    val editText = remember {
        EditText(context).apply {
            inputType = EditorInfo.TYPE_CLASS_TEXT
            privateImeOptions = "org.futo.inputmethod.latin.NoSuggestions=1"
            isSingleLine = true
            background = null
        }
    }

    LaunchedEffect(Unit) {
        delay(50L)
        editText.requestFocus()
        (context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager)
            .showSoftInput(editText, InputMethodManager.SHOW_IMPLICIT)
    }

    ScrollableList(horizontalAlignment = Alignment.CenterHorizontally) {
        ScreenTitle(stringResource(R.string.key_hints_title), showBack = true)

        AndroidView({ editText }, modifier = Modifier.fillMaxWidth().height(1.dp))

        SettingSlider(
            title = stringResource(R.string.keyboard_settings_key_hint_size),
            subtitle = stringResource(R.string.key_hints_size_subtitle),
            setting = KEY_HINT_SCALE,
            range = 0.0f .. 2.0f,
            transform = { (it * 100f).roundToInt() / 100f },
            indicator = { "${(it * 100f).roundToInt()}%" }
        )
        SettingSlider(
            title = stringResource(R.string.keyboard_settings_key_hint_brightness),
            subtitle = stringResource(R.string.key_hints_brightness_subtitle),
            setting = KEY_HINT_BRIGHTNESS_PERCENT,
            range = 0.0f .. 200.0f,
            transform = { (it / 5f).roundToInt() * 5 },
            indicator = { "$it%" }
        )
    }
}
