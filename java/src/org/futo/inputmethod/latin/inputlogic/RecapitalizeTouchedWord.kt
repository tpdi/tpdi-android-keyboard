package org.futo.inputmethod.latin.inputlogic

import org.futo.inputmethod.latin.RichInputConnection
import org.futo.inputmethod.latin.settings.SettingsValues

/**
 * Lets Shift recapitalize the word the cursor is touching when nothing is selected, by selecting
 * that word first. The stock recapitalization ([InputLogic.performRecapitalization]) then runs
 * unchanged on the selection. Does nothing while the flag is off or when text is selected.
 * Returns true when it selected a word.
 */
object RecapitalizeTouchedWord {
    @Volatile
    private var enabled = false

    @JvmStatic
    fun onSettingChanged(value: Boolean) {
        enabled = value
    }

    @JvmStatic
    fun selectWordIfTouching(
        connection: RichInputConnection,
        settingsValues: SettingsValues,
        scriptId: Int
    ): Boolean {
        if (!enabled || connection.hasSelection()) return false
        if (!settingsValues.mSpacingAndPunctuations.currentLanguageHasSpaces) return false

        val range = connection.getWordRangeAtCursor(settingsValues, scriptId, true) ?: return false
        if (range.length() <= 0 || range.mHasUrlSpans) return false

        val cursor = connection.expectedSelectionStart
        val start = cursor - range.numberOfCharsInWordBeforeCursor
        val end = cursor + range.numberOfCharsInWordAfterCursor
        if (start < 0 || end <= start) return false

        connection.finishComposingText()
        connection.setSelection(start, end)
        return true
    }
}
