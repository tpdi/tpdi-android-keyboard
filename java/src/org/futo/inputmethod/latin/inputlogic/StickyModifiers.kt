package org.futo.inputmethod.latin.inputlogic

import android.view.KeyCharacterMap
import android.view.KeyEvent
import org.futo.inputmethod.latin.common.Constants

/**
 * Ctrl and Alt as sticky keys. Tapping a modifier key latches it; the next key is sent as a real
 * key event with the modifier set, and the latch then clears. Latching again before that key
 * unlatches it.
 */
object StickyModifiers {
    @Volatile private var ctrl = false
    @Volatile private var alt = false

    @JvmStatic
    val active: Boolean get() = ctrl || alt

    @JvmStatic
    fun toggle(layoutCode: Int) {
        when (layoutCode) {
            Constants.CODE_CTRL -> ctrl = !ctrl
            Constants.CODE_ALT -> alt = !alt
        }
    }

    @JvmStatic
    fun clear() {
        ctrl = false
        alt = false
    }

    /** The meta state of the latched modifiers; clears them. */
    @JvmStatic
    fun take(): Int {
        val meta = (if (ctrl) KeyEvent.META_CTRL_ON else 0) or (if (alt) KeyEvent.META_ALT_ON else 0)
        clear()
        return meta
    }

    /**
     * The Android key code and meta state a hardware keyboard would produce for [codePoint], or
     * null if it has no key (for example most emoji).
     */
    @JvmStatic
    fun keyEventFor(codePoint: Int): Pair<Int, Int>? {
        val events = KeyCharacterMap.load(KeyCharacterMap.VIRTUAL_KEYBOARD)
            .getEvents(Character.toChars(codePoint)) ?: return null
        // The events are [shift down?] key down, key up [shift up?]; the key is the one that
        // is not a bare modifier.
        val key = events.firstOrNull {
            it.action == KeyEvent.ACTION_DOWN && !KeyEvent.isModifierKey(it.keyCode)
        } ?: return null
        return key.keyCode to key.metaState
    }
}
