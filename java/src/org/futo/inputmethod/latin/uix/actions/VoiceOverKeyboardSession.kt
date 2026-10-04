package org.futo.inputmethod.latin.uix.actions

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import org.futo.inputmethod.latin.uix.ActionInputTransaction
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.TypedTextTap

/**
 * The state of one voice input session that depends on how it is shown (in the full voice
 * window, or over the keyboard with typing still working): which kind of input transaction is
 * in use, what was typed while a segment was being transcribed, and the Undo history.
 * [VoiceInputActionWindow] owns one and delegates to it.
 */
internal class VoiceOverKeyboardSession(
    private val manager: KeyboardManagerForAction,
    val inlineMode: Boolean
) {
    /** What this session has committed (and typed), so Undo can take it back one unit at a time. */
    val undoHistory = VoiceUndoHistory()

    private fun newTransaction() =
        if (inlineMode) manager.createUnroutedInputTransaction() else manager.createInputTransaction()

    var transaction: ActionInputTransaction = newTransaction()

    // Typing while a segment is being transcribed: the spoken text arrives after the keys, so
    // it would land after them. Remember what was typed since the segment was handed off, and
    // when the result arrives put the spoken text first and retype that text after it.
    private var typedWhilePending: StringBuilder? = null
    private var undoIndexAtSegmentStart = 0

    init {
        if (inlineMode) TypedTextTap.listener = { cp, del -> onTypedEvent(cp, del) }
    }

    fun segmentStarted() {
        if (!inlineMode) return
        typedWhilePending = StringBuilder()
        undoIndexAtSegmentStart = undoHistory.size
    }

    private fun onTypedEvent(codePoint: Int, isDelete: Boolean) {
        typedWhilePending?.let { pending ->
            if (isDelete) {
                if (pending.isNotEmpty()) pending.setLength(pending.length - 1)
            } else {
                pending.appendCodePoint(codePoint)
            }
        }
        undoHistory.onTypedEvent(codePoint, isDelete)
    }

    /** Commits a finished segment, putting anything typed meanwhile after it. */
    fun commitSegment(committedText: String) {
        val typed = typedWhilePending?.toString() ?: ""
        typedWhilePending = null
        if (typed.isNotEmpty()) {
            // Take back what was typed while this was being transcribed; it goes after.
            transaction.finishComposingText()
            transaction.deleteTextBeforeCursor(typed.length)
        }
        // Committed for good, no later revision: start a fresh transaction so the
        // next segment's partial/commit calls don't touch what's already locked in.
        transaction.commit(committedText)
        transaction = newTransaction()
        if (typed.isNotEmpty()) {
            transaction.commit(typed)
            transaction = newTransaction()
            // Undo history follows the on-screen order: spoken text, then typed.
            undoHistory.insertVoiceEntryAt(undoIndexAtSegmentStart, committedText)
        } else {
            undoHistory.pushVoiceEntry(committedText)
        }
    }

    /** A segment result that is blank: forget what was typed, nothing is reordered. */
    fun segmentEmpty() {
        typedWhilePending = null
    }

    fun undoLast() {
        manager.getLifecycleScope().launch(Dispatchers.Main) {
            undoHistory.undoLast(transaction)
        }
    }

    fun close() {
        TypedTextTap.listener = null
    }
}
