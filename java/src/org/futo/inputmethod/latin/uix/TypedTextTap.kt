package org.futo.inputmethod.latin.uix

import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.common.Constants

/**
 * Lets an active over-the-keyboard voice session see typed characters and backspaces, so typed
 * text can go into the same Undo history as dictated text. Does nothing when no listener is set.
 */
object TypedTextTap {
    @Volatile
    var listener: ((codePoint: Int, isDelete: Boolean) -> Unit)? = null

    fun dispatch(event: Event) {
        val l = listener ?: return
        if (event.mKeyCode == Constants.CODE_DELETE) {
            l(-1, true)
        } else if (event.eventType == Event.EVENT_TYPE_INPUT_KEYPRESS && event.mCodePoint > 0) {
            l(event.mCodePoint, false)
        }
    }
}
