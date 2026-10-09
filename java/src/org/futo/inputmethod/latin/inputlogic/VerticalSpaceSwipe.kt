package org.futo.inputmethod.latin.inputlogic

import android.view.KeyEvent

/**
 * Up and down cursor movement while dragging on the space bar. [PointerTracker] asks how many
 * lines the finger has moved and sends them as Up or Down key events. Does nothing while the
 * setting is off.
 */
object VerticalSpaceSwipe {
    @Volatile
    private var enabled = false

    /** Set by the keyboard service: sends a down+up key event with the given key code. */
    @Volatile
    var sender: ((Int) -> Unit)? = null

    @JvmStatic
    fun onSettingChanged(value: Boolean) {
        enabled = value
    }

    @JvmStatic
    fun isEnabled(): Boolean = enabled

    /** Whole lines moved for a vertical finger movement of [dy] pixels (up is negative). */
    @JvmStatic
    fun lines(dy: Int, stepPx: Int): Int = if (stepPx <= 0) 0 else dy / stepPx

    /** Sends [lines] Up key events when negative, Down key events when positive. */
    @JvmStatic
    fun send(lines: Int) {
        val code = if (lines < 0) KeyEvent.KEYCODE_DPAD_UP else KeyEvent.KEYCODE_DPAD_DOWN
        repeat(kotlin.math.abs(lines)) { sender?.invoke(code) }
    }
}
