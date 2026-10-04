package org.futo.inputmethod.latin.inputlogic

import android.view.KeyEvent
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.event.InputTransaction
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

    /** True for keys InputLogic handles as special keys (Tab is handled with the text keys). */
    val isSpecial: Boolean get() = this != NONE && this != TAB
}

/** What the special keys do. One implementation per state of the "send key codes" setting. */
interface SpecialKeyBehavior {
    /** Escape, Home, End, Page Up/Down, Forward Delete, Insert, F1-F12. */
    fun handleSpecial(layoutCode: Int, logic: InputLogic, transaction: InputTransaction)

    /** Tab. Returns false when the caller should type it like any other character. */
    fun handleTab(logic: InputLogic, transaction: InputTransaction): Boolean

    /** Whether Enter is sent as a real key event. */
    val enterAsKeyEvent: Boolean
}

/** Setting on: the keys are sent as real key events, like a hardware keyboard. */
private object KeyEventBehavior : SpecialKeyBehavior {
    override fun handleSpecial(layoutCode: Int, logic: InputLogic, transaction: InputTransaction) {
        logic.commitTyped(transaction.mSettingsValues, "")
        logic.sendDownUpKeyEvent(SpecialKeyEvents.androidKeyCodeFor(layoutCode), 0)
    }

    override fun handleTab(logic: InputLogic, transaction: InputTransaction): Boolean {
        logic.commitTyped(transaction.mSettingsValues, "")
        logic.sendDownUpKeyEvent(SpecialKey.TAB.keyEvent, 0)
        return true
    }

    override val enterAsKeyEvent = true
}

/** Setting off (the default): Escape types an ESC character, the other special keys do nothing. */
private object TextBehavior : SpecialKeyBehavior {
    override fun handleSpecial(layoutCode: Int, logic: InputLogic, transaction: InputTransaction) {
        logic.commitTyped(transaction.mSettingsValues, "")
        SpecialKeyEvents.textFallbackFor(layoutCode)?.let { logic.mConnection.commitText(it, 1) }
    }

    override fun handleTab(logic: InputLogic, transaction: InputTransaction) = false
    override val enterAsKeyEvent = false
}

object SpecialKeyEvents {
    private val byLayoutCode: Map<Int, SpecialKey> =
        SpecialKey.values().filter { it != SpecialKey.NONE }.associateBy { it.layoutCode }

    /** The behavior for the current state of the setting. Swapped, not checked per key press. */
    @Volatile
    @JvmStatic
    var behavior: SpecialKeyBehavior = TextBehavior
        private set

    /** Called at startup and whenever the "send key codes rather than text" setting changes. */
    @JvmStatic
    fun onSettingChanged(sendKeyCodes: Boolean) {
        behavior = if (sendKeyCodes) KeyEventBehavior else TextBehavior
    }

    /** Handles [event] if it is one of the special keys; returns whether it did. */
    @JvmStatic
    fun handle(logic: InputLogic, event: Event, transaction: InputTransaction): Boolean {
        if (!lookup(event.mKeyCode).isSpecial) return false
        transaction.setRequiresUpdateSuggestions()
        behavior.handleSpecial(event.mKeyCode, logic, transaction)
        return true
    }

    /** Handles Tab; returns false if it should be typed as a normal character. */
    @JvmStatic
    fun handleTab(logic: InputLogic, transaction: InputTransaction): Boolean =
        behavior.handleTab(logic, transaction)

    private fun lookup(layoutCode: Int): SpecialKey = byLayoutCode[layoutCode] ?: SpecialKey.NONE

    /** The Android key code for a layout key code, or -1 if the key is not in this class. */
    @JvmStatic
    fun androidKeyCodeFor(layoutCode: Int): Int = lookup(layoutCode).keyEvent

    /** What to insert when the setting is off, or null for nothing. */
    @JvmStatic
    fun textFallbackFor(layoutCode: Int): String? = lookup(layoutCode).textFallback
}
