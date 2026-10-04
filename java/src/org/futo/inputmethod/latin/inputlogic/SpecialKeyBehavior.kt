package org.futo.inputmethod.latin.inputlogic

/** What [SpecialKeyBehavior] needs from the input logic to act on a key. */
interface SpecialKeyOutput {
    fun commitTyped()
    fun sendKeyEvent(androidKeyCode: Int)
    fun commitText(text: String)
}

/**
 * What the special keys do. There is one implementation per state of the "send key codes rather
 * than text" setting; [SpecialKeyEvents] holds the active one and swaps it when the setting
 * changes, so nothing checks the setting per key press.
 */
interface SpecialKeyBehavior {
    /** Escape, Home, End, Page Up/Down, Forward Delete, Insert, F1-F12. */
    fun handleSpecial(layoutCode: Int, out: SpecialKeyOutput)

    /** Tab. Returns false when the caller should type it like any other character. */
    fun handleTab(out: SpecialKeyOutput): Boolean

    /** Enter. Returns false when the caller should handle it as usual. */
    fun handleEnter(out: SpecialKeyOutput): Boolean
}

/** Setting on: the keys are sent as real key events, like a hardware keyboard. */
object KeyEventBehavior : SpecialKeyBehavior {
    override fun handleSpecial(layoutCode: Int, out: SpecialKeyOutput) {
        out.commitTyped()
        out.sendKeyEvent(SpecialKeyEvents.androidKeyCodeFor(layoutCode))
    }

    override fun handleTab(out: SpecialKeyOutput): Boolean {
        out.commitTyped()
        out.sendKeyEvent(SpecialKey.TAB.keyEvent)
        return true
    }

    override fun handleEnter(out: SpecialKeyOutput): Boolean {
        out.sendKeyEvent(android.view.KeyEvent.KEYCODE_ENTER)
        return true
    }
}

/** Setting off (the default): Escape types an ESC character, the other special keys do nothing. */
object TextBehavior : SpecialKeyBehavior {
    override fun handleSpecial(layoutCode: Int, out: SpecialKeyOutput) {
        out.commitTyped()
        SpecialKeyEvents.textFallbackFor(layoutCode)?.let { out.commitText(it) }
    }

    override fun handleTab(out: SpecialKeyOutput): Boolean = false
    override fun handleEnter(out: SpecialKeyOutput): Boolean = false
}
