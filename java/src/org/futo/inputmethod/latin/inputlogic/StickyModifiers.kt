package org.futo.inputmethod.latin.inputlogic

import android.view.KeyCharacterMap
import android.view.KeyEvent
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.event.InputTransaction
import org.futo.inputmethod.latin.common.Constants

/**
 * Ctrl, Alt, Meta and AltGr as sticky keys. Tapping a modifier key latches it; the next key is sent as a real
 * key event with the modifier set, and the latch then clears. Latching again before that key
 * unlatches it.
 */
object StickyModifiers {
    /** The "Sticky modifier keys" setting; set by [onSettingChanged], not read per key press. */
    @Volatile private var enabled = false

    /** Called at startup and whenever the setting changes. Turning it off releases any latch. */
    @JvmStatic
    fun onSettingChanged(enabled: Boolean) {
        this.enabled = enabled
        if (!enabled) clearAll()
    }

    /**
     * Handles the modifier keys (they only latch when the setting is on; with it off they do
     * nothing), and Backspace while a modifier is latched. Returns whether it handled the event.
     */
    @JvmStatic
    fun handleKey(logic: InputLogic, event: Event, transaction: InputTransaction): Boolean {
        // Escape, Home, End, Page Up/Down, Forward Delete, Insert, F1-F12: sent with the modifiers.
        val special = SpecialKeyEvents.androidKeyCodeFor(event.mKeyCode)
        if (special != -1 && active) {
            logic.commitTyped(transaction.mSettingsValues, "")
            logic.sendDownUpKeyEvent(special, take())
            transaction.setDidAffectContents()
            return true
        }
        when (event.mKeyCode) {
            in metaFor -> {
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

    /**
     * Layout code to the meta state it sends, as a hardware keyboard would: the generic flag plus
     * the left or right one. AltGr is what a hardware keyboard reports as right Alt.
     */
    private val metaFor = mapOf(
        Constants.CODE_CTRL to (KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_LEFT_ON),
        Constants.CODE_ALT to (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_LEFT_ON),
        Constants.CODE_META to (KeyEvent.META_META_ON or KeyEvent.META_META_LEFT_ON),
        Constants.CODE_ALTGR to (KeyEvent.META_ALT_ON or KeyEvent.META_ALT_RIGHT_ON),
        Constants.CODE_FN to KeyEvent.META_FUNCTION_ON,
        Constants.CODE_SYM to KeyEvent.META_SYM_ON,
        Constants.CODE_STICKY_SHIFT to (KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_LEFT_ON),
        Constants.CODE_CAPS_LOCK_MOD to KeyEvent.META_CAPS_LOCK_ON,
        Constants.CODE_NUMLOCK to KeyEvent.META_NUM_LOCK_ON,
        Constants.CODE_SCROLLLOCK to KeyEvent.META_SCROLL_LOCK_ON,
        Constants.CODE_CTRL_RIGHT to (KeyEvent.META_CTRL_ON or KeyEvent.META_CTRL_RIGHT_ON),
        Constants.CODE_SHIFT_RIGHT to (KeyEvent.META_SHIFT_ON or KeyEvent.META_SHIFT_RIGHT_ON),
        Constants.CODE_META_RIGHT to (KeyEvent.META_META_ON or KeyEvent.META_META_RIGHT_ON),
    )

    /** Latched for the next key only (tap). */
    @Volatile private var latched = emptySet<Int>()

    /** Held until the key is tapped again (long press). */
    @Volatile private var locked = emptySet<Int>()

    @JvmStatic
    val active: Boolean get() = latched.isNotEmpty() || locked.isNotEmpty()

    @JvmStatic
    fun isModifierKey(layoutCode: Int): Boolean = enabled && layoutCode in metaFor

    /**
     * A long press locks the key until it is tapped again. Returns whether it was handled, in
     * which case the key press is consumed.
     */
    @JvmStatic
    fun onLongPress(layoutCode: Int): Boolean {
        if (!isModifierKey(layoutCode)) return false
        latched = latched - layoutCode
        locked = locked + layoutCode
        onChanged?.run()
        return true
    }

    /** Called after the latched state changes, so the keyboard can redraw the key labels. */
    @Volatile
    @JvmStatic
    var onChanged: Runnable? = null

    /** Whether the key with this layout code is currently latched. */
    @JvmStatic
    fun isLatched(layoutCode: Int): Boolean = layoutCode in latched || layoutCode in locked

    @JvmStatic
    fun toggle(layoutCode: Int) {
        if (layoutCode !in metaFor) return
        if (layoutCode in locked) {
            locked = locked - layoutCode
        } else {
            latched = if (layoutCode in latched) latched - layoutCode else latched + layoutCode
        }
        onChanged?.run()
    }

    /** Releases the keys latched for the next key; locked keys stay. */
    @JvmStatic
    fun clear() {
        if (latched.isEmpty()) return
        latched = emptySet()
        onChanged?.run()
    }

    /** Releases everything, including locked keys. */
    @JvmStatic
    fun clearAll() {
        val changed = active
        latched = emptySet()
        locked = emptySet()
        if (changed) onChanged?.run()
    }

    /** The meta state of the latched modifiers; clears them. */
    @JvmStatic
    fun take(): Int {
        val state = (latched + locked).fold(0) { acc, code -> acc or (metaFor[code] ?: 0) }
        clear()
        return state
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
