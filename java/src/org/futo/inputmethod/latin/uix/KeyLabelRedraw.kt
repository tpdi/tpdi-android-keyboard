package org.futo.inputmethod.latin.uix

import android.content.Context
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.drop

// Redraws the keyboard when the key label size changes, so the slider takes effect while the
// keyboard is on screen.
object KeyLabelRedraw {
    suspend fun watch(context: Context, redraw: () -> Unit) {
        context.getSettingFlow(KEY_LABEL_SCALE)
            .distinctUntilChanged()
            .drop(1)
            .collect { redraw() }
    }
}
