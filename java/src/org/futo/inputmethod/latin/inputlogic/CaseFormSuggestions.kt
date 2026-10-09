package org.futo.inputmethod.latin.inputlogic

import org.futo.inputmethod.latin.Dictionary
import org.futo.inputmethod.latin.SuggestedWords
import org.futo.inputmethod.latin.SuggestedWords.SuggestedWordInfo
import org.futo.inputmethod.latin.common.StringUtils
import java.util.Locale

/**
 * Builds the suggestion strip shown when Shift is tapped while the cursor is in or next to a word: the word
 * as it is, followed by its other case forms (Capitalized, UPPER, lower), one more form per tap
 * on the same word, newest first. Picking one replaces the word through the normal suggestion
 * pick. Does nothing while the flag is off.
 */
object CaseFormSuggestions {
    @Volatile
    private var enabled = false

    private var lastWord: String? = null
    private var lastCursor = -1
    private var taps = 0

    @JvmStatic
    fun onSettingChanged(value: Boolean) {
        enabled = value
    }

    /** Returns the suggestions to show, or null to leave Shift to its normal behaviour. */
    @JvmStatic
    fun onShift(word: String?, cursor: Int, hasSelection: Boolean, locale: Locale): SuggestedWords? {
        if (!enabled || hasSelection || word.isNullOrEmpty()) return null

        val forms = listOf(
            StringUtils.capitalizeFirstCodePoint(word.lowercase(locale), locale),
            word.uppercase(locale),
            word.lowercase(locale)
        ).filter { it != word }.distinct()
        if (forms.isEmpty()) return null

        taps = if (word == lastWord && cursor == lastCursor) taps % forms.size + 1 else 1
        lastWord = word
        lastCursor = cursor

        val typed = SuggestedWordInfo(
            word, "", SuggestedWordInfo.MAX_SCORE, SuggestedWordInfo.KIND_TYPED,
            Dictionary.DICTIONARY_USER_TYPED, SuggestedWordInfo.NOT_AN_INDEX,
            SuggestedWordInfo.NOT_A_CONFIDENCE
        )
        val list = ArrayList<SuggestedWordInfo>()
        list.add(typed)
        forms.take(taps).asReversed().forEachIndexed { i, form ->
            list.add(
                SuggestedWordInfo(
                    form, "", SuggestedWordInfo.MAX_SCORE - 1 - i,
                    SuggestedWordInfo.KIND_CORRECTION, Dictionary.DICTIONARY_USER_TYPED,
                    SuggestedWordInfo.NOT_AN_INDEX, SuggestedWordInfo.NOT_A_CONFIDENCE
                )
            )
        }
        return SuggestedWords(
            list, null, typed, false /* typedWordValid */, false /* willAutoCorrect */,
            false /* isObsoleteSuggestions */, SuggestedWords.INPUT_STYLE_TYPING,
            SuggestedWords.NOT_A_SEQUENCE_NUMBER
        )
    }
}
