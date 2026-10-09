package org.futo.inputmethod.latin.inputlogic

import org.futo.inputmethod.latin.RichInputConnection
import org.futo.inputmethod.latin.settings.SettingsValues

/**
 * Lets Shift recapitalize the word the cursor is touching when nothing is selected, by selecting
 * that word first. The stock recapitalization ([InputLogic.performRecapitalization]) then runs
 * unchanged on the selection. Does nothing while the flag is off or when text is selected.
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
    ) {
        if (!enabled || connection.hasSelection()) return
        if (!settingsValues.mSpacingAndPunctuations.currentLanguageHasSpaces) return

        val range = connection.getWordRangeAtCursor(settingsValues, scriptId, true) ?: return
        if (range.length() <= 0 || range.mHasUrlSpans) return

        val cursor = connection.expectedSelectionStart
        val start = cursor - range.numberOfCharsInWordBeforeCursor
        val end = cursor + range.numberOfCharsInWordAfterCursor
        if (start < 0 || end <= start) return

        connection.finishComposingText()
        connection.setSelection(start, end)
    }
}
