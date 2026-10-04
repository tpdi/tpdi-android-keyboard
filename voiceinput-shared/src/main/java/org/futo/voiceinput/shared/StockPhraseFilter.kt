package org.futo.voiceinput.shared

import android.util.Log

/**
 * The speech model produces phrases like "Thanks for watching" from silence or noise, evidently
 * from video subtitles in its training data. A result that is exactly one of these is dropped.
 * Only whole-result matches count, so the phrase inside a longer sentence is left alone.
 */
object StockPhraseFilter {
    private val STOCK_PHRASES = setOf(
        "thanks for watching", "thank you for watching", "thanks for watching and see you next time",
        "please subscribe", "like and subscribe", "subscribe to my channel",
        "see you in the next video", "see you next time", "subtitles by the amaraorg community"
    )

    private fun normalized(text: String) =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}' ]"), " ").trim().replace(Regex("\\s+"), " ")

    fun isStockPhrase(text: String) = normalized(text) in STOCK_PHRASES

    /** [text], or an empty string if it is a stock phrase and [enabled]. [where] is for the log. */
    fun filter(enabled: Boolean, text: String, where: String): String {
        if (!isStockPhrase(text)) return text
        Log.d("StockPhrase", "$where result is a stock phrase [$text]; dropping=$enabled")
        return if (enabled) "" else text
    }
}
