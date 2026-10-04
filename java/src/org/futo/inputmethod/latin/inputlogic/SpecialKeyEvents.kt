package org.futo.inputmethod.latin.inputlogic

import android.view.KeyEvent
import org.futo.inputmethod.latin.common.Constants

/**
 * Keys that can be sent either as text or as a real key event, the way a hardware keyboard
 * sends them. Which one is used is decided by the "send key codes rather than text" setting;
 * [InputLogic] asks this object only when that setting is on, or for keys that have no text
 * form at all.
 */
object SpecialKeyEvents {
    private const val NONE = -1

    /** The Android key code for a layout key code, or -1 if the key is not in this class. */
    @JvmStatic
    fun androidKeyCodeFor(layoutCode: Int): Int = when (layoutCode) {
        Constants.CODE_ESCAPE -> KeyEvent.KEYCODE_ESCAPE
        Constants.CODE_HOME -> KeyEvent.KEYCODE_MOVE_HOME
        Constants.CODE_END -> KeyEvent.KEYCODE_MOVE_END
        Constants.CODE_PAGE_UP -> KeyEvent.KEYCODE_PAGE_UP
        Constants.CODE_PAGE_DOWN -> KeyEvent.KEYCODE_PAGE_DOWN
        Constants.CODE_FORWARD_DELETE -> KeyEvent.KEYCODE_FORWARD_DEL
        Constants.CODE_INSERT -> KeyEvent.KEYCODE_INSERT
        Constants.CODE_TAB -> KeyEvent.KEYCODE_TAB
        in Constants.CODE_F12..Constants.CODE_F1 ->
            KeyEvent.KEYCODE_F1 + (Constants.CODE_F1 - layoutCode)
        else -> NONE
    }

    /** True for keys that exist only as key events (no text form, apart from Escape). */
    @JvmStatic
    fun isKeyEventOnly(layoutCode: Int): Boolean =
        layoutCode != Constants.CODE_TAB && androidKeyCodeFor(layoutCode) != NONE

    /** What to insert when the setting is off, or null for nothing (only Escape has a text form). */
    @JvmStatic
    fun textFallbackFor(layoutCode: Int): String? =
        if (layoutCode == Constants.CODE_ESCAPE) "\u001B" else null
}
