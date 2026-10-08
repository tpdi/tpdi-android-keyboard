package org.futo.inputmethod.latin.uix

import android.content.Context
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

// Redraws the keyboard when a key hint setting changes, so the sliders on the Key hints page
// take effect on the keyboard while it is on screen.
object KeyHintsRedraw {
    suspend fun watch(context: Context, redraw: () -> Unit) {
        combine(
            context.getSettingFlow(KEY_HINT_SCALE_PERCENT),
            context.getSettingFlow(KEY_HINT_BRIGHTNESS_PERCENT)
        ) { size, brightness -> size to brightness }
            .distinctUntilChanged()
            .drop(1)
            .collect { redraw() }
    }
}
