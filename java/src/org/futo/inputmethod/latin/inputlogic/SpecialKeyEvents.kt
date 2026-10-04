package org.futo.inputmethod.latin.inputlogic

import android.view.KeyEvent
import org.futo.inputmethod.latin.common.Constants

/**
 * Keys that can be sent either as text or as a real key event, the way a hardware keyboard
 * sends them. Which one is used is decided by the "send key codes rather than text" setting;
 * [InputLogic] asks this object only when that setting is on, or for keys that have no text
 * form at all.
 *
 * [textFallback] is what to insert when the setting is off; null means the key does nothing then.
 */
enum class SpecialKey(val layoutCode: Int, val keyEvent: Int, val textFallback: String?) {
    NONE(Constants.CODE_UNSPECIFIED, -1, null),
    ESCAPE(Constants.CODE_ESCAPE, KeyEvent.KEYCODE_ESCAPE, "\u001B"),
    HOME(Constants.CODE_HOME, KeyEvent.KEYCODE_MOVE_HOME, null),
    END(Constants.CODE_END, KeyEvent.KEYCODE_MOVE_END, null),
    PAGE_UP(Constants.CODE_PAGE_UP, KeyEvent.KEYCODE_PAGE_UP, null),
    PAGE_DOWN(Constants.CODE_PAGE_DOWN, KeyEvent.KEYCODE_PAGE_DOWN, null),
    FORWARD_DELETE(Constants.CODE_FORWARD_DELETE, KeyEvent.KEYCODE_FORWARD_DEL, null),
    INSERT(Constants.CODE_INSERT, KeyEvent.KEYCODE_INSERT, null),
    TAB(Constants.CODE_TAB, KeyEvent.KEYCODE_TAB, "\t"),
    F1(Constants.CODE_F1, KeyEvent.KEYCODE_F1, null),
    F2(Constants.CODE_F2, KeyEvent.KEYCODE_F2, null),
    F3(Constants.CODE_F3, KeyEvent.KEYCODE_F3, null),
    F4(Constants.CODE_F4, KeyEvent.KEYCODE_F4, null),
    F5(Constants.CODE_F5, KeyEvent.KEYCODE_F5, null),
    F6(Constants.CODE_F6, KeyEvent.KEYCODE_F6, null),
    F7(Constants.CODE_F7, KeyEvent.KEYCODE_F7, null),
    F8(Constants.CODE_F8, KeyEvent.KEYCODE_F8, null),
    F9(Constants.CODE_F9, KeyEvent.KEYCODE_F9, null),
    F10(Constants.CODE_F10, KeyEvent.KEYCODE_F10, null),
    F11(Constants.CODE_F11, KeyEvent.KEYCODE_F11, null),
    F12(Constants.CODE_F12, KeyEvent.KEYCODE_F12, null);

    /** True for keys that exist only as key events (Escape and Tab have a text form). */
    val isKeyEventOnly: Boolean get() = this != NONE && textFallback == null
}

object SpecialKeyEvents {
    private val byLayoutCode: Map<Int, SpecialKey> =
        SpecialKey.values().filter { it != SpecialKey.NONE }.associateBy { it.layoutCode }

    private fun lookup(layoutCode: Int): SpecialKey = byLayoutCode[layoutCode] ?: SpecialKey.NONE

    /** The Android key code for a layout key code, or -1 if the key is not in this class. */
    @JvmStatic
    fun androidKeyCodeFor(layoutCode: Int): Int = lookup(layoutCode).keyEvent

    /** True for keys that exist only as key events (no text form at all). */
    @JvmStatic
    fun isKeyEventOnly(layoutCode: Int): Boolean = lookup(layoutCode).isKeyEventOnly

    /** What to insert when the setting is off, or null for nothing. */
    @JvmStatic
    fun textFallbackFor(layoutCode: Int): String? = lookup(layoutCode).textFallback
}
