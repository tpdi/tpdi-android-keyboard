package org.futo.inputmethod.latin.inputlogic

import android.view.KeyCharacterMap
import android.view.KeyEvent
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.event.InputTransaction
import org.futo.inputmethod.latin.common.Constants

/**
 * Ctrl and Alt as sticky keys. Tapping a modifier key latches it; the next key is sent as a real
 * key event with the modifier set, and the latch then clears. Latching again before that key
 * unlatches it.
 */
object StickyModifiers {
    /** The "Sticky Ctrl and Alt keys" setting; set by [onSettingChanged], not read per key press. */
    @Volatile private var enabled = false

    /** Called at startup and whenever the setting changes. Turning it off releases any latch. */
    @JvmStatic
    fun onSettingChanged(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) clear()
    }

    /**
     * Handles the Ctrl and Alt keys (they only latch when the setting is on; with it off they do
     * nothing), and Backspace while a modifier is latched. Returns whether it handled the event.
     */
    @JvmStatic
    fun handleKey(logic: InputLogic, event: Event, transaction: InputTransaction): Boolean {
        when (event.mKeyCode) {
            Constants.CODE_CTRL, Constants.CODE_ALT -> {
                if (enabled) toggle(event.mKeyCode)
                return true
            }
            Constants.CODE_DELETE -> {
                if (!active) return false
                logic.commitTyped(transaction.mSettingsValues, "")
                logic.sendDownUpKeyEvent(KeyEvent.KEYCODE_DEL, take())
                transaction.setDidAffectContents()
                return true
            }
        }
        return false
    }

    /**
     * Sends a typed character as a key event carrying the latched modifiers. Returns false if
     * nothing is latched or the character has no hardware key, so it is typed normally.
     */
    @JvmStatic
    fun handleCharacter(logic: InputLogic, event: Event, transaction: InputTransaction): Boolean {
        if (!active) return false
        val key = keyEventFor(event.mCodePoint) ?: return false
        logic.commitTyped(transaction.mSettingsValues, "")
        logic.sendDownUpKeyEvent(key.first, key.second or take())
        return true
    }

    /** A latched modifier key shows its label in capitals. */
    @JvmStatic
    fun labelFor(layoutCode: Int, label: String?): String? =
        if (label != null && isLatched(layoutCode)) label.uppercase() else label

    @Volatile private var ctrl = false
    @Volatile private var alt = false

    @JvmStatic
    val active: Boolean get() = ctrl || alt

    /** Called after the latched state changes, so the keyboard can redraw the key labels. */
    @Volatile
    @JvmStatic
    var onChanged: Runnable? = null

    /** Whether the key with this layout code is currently latched. */
    @JvmStatic
    fun isLatched(layoutCode: Int): Boolean = when (layoutCode) {
        Constants.CODE_CTRL -> ctrl
        Constants.CODE_ALT -> alt
        else -> false
    }

    @JvmStatic
    fun toggle(layoutCode: Int) {
        when (layoutCode) {
            Constants.CODE_CTRL -> ctrl = !ctrl
            Constants.CODE_ALT -> alt = !alt
        }
        onChanged?.run()
    }

    @JvmStatic
    fun clear() {
        val changed = ctrl || alt
        ctrl = false
        alt = false
        if (changed) onChanged?.run()
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
