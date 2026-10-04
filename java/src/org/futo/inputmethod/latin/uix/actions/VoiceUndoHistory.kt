package org.futo.inputmethod.latin.uix.actions

import org.futo.inputmethod.latin.uix.ActionInputTransaction

/**
 * Undo history for one voice input session: what dictation committed, and what was typed
 * meanwhile, most recent last. Undo removes one entry at a time, repeatably.
 *
 * Typed characters are grouped into a word (closed by whitespace); a typed backspace shortens
 * the latest entry, so the keyboard's backspace and Undo stay consistent. No attempt is made to
 * guard against typing and dictation interleaving.
 */
class VoiceUndoHistory {
    private val entries = mutableListOf<String>()
    private var openTypedEntry = false

    val size: Int get() = entries.size

    /** Text that dictation itself committed (a segment, an Enter). */
    fun pushVoiceEntry(text: String) {
        entries.add(text)
        openTypedEntry = false
    }

    /** A character typed on the keyboard, or [isDelete] for a backspace. */
    fun onTypedEvent(codePoint: Int, isDelete: Boolean) {
        if (isDelete) {
            val last = entries.lastOrNull() ?: return
            if (last.length <= 1) {
                entries.removeAt(entries.lastIndex)
                openTypedEntry = false
            } else {
                entries[entries.lastIndex] = last.dropLast(1)
            }
            return
        }
        val ch = String(Character.toChars(codePoint))
        if (openTypedEntry && entries.isNotEmpty()) {
            entries[entries.lastIndex] = entries.last() + ch
        } else {
            entries.add(ch)
        }
        openTypedEntry = !Character.isWhitespace(codePoint)
    }

    /**
     * Puts [voiceEntry] at position [index], moving everything recorded from that position on
     * behind it: the on-screen order when text typed while a segment was being transcribed ends
     * up after the spoken words.
     */
    fun insertVoiceEntryAt(index: Int, voiceEntry: String) {
        val from = index.coerceIn(0, entries.size)
        val after = entries.subList(from, entries.size).toList()
        while (entries.size > from) entries.removeAt(entries.lastIndex)
        entries.add(voiceEntry)
        entries.addAll(after)
        openTypedEntry = false
    }

    /** Removes the latest entry from the text before the cursor, if there is one. */
    fun undoLast(transaction: ActionInputTransaction) {
        val text = entries.lastOrNull() ?: run { android.util.Log.d("VoiceUndo", "nothing to undo"); return }
        android.util.Log.d("VoiceUndo", "undoing [${text.replace("\n", "\\n")}] len=${text.length} remaining=${entries.size - 1}")
        entries.removeAt(entries.lastIndex)
        openTypedEntry = false
        transaction.deleteTextBeforeCursor(text.length)
    }
}
