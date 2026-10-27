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
        "see you in the next video", "see you next time", "subtitles by the amaraorg community",
        "thanks for subscribing", "thank you for subscribing", "thanks for subscribing see you next time",
        "i'll see you next time", "ill see you next time", "see you in the next one", "see you later"
    )

    // Phrases cut off the end of a longer result. "Thank you" alone is not here: it is often real.
    private val TRAILING = STOCK_PHRASES.map { phrase ->
        Regex("(?:^|[^\\p{L}\\p{N}'])" + phrase.split(" ").joinToString("[^\\p{L}\\p{N}']+") { Regex.escape(it) } +
            "[^\\p{L}\\p{N}']*$", RegexOption.IGNORE_CASE)
    }

    /** [text] with stock phrases cut off its end, repeatedly, if [enabled]. */
    fun stripTrailing(enabled: Boolean, text: String, where: String): String {
        if (!enabled) return text
        var out = text
        while (true) {
            val m = TRAILING.firstNotNullOfOrNull { it.find(out) } ?: break
            out = out.substring(0, m.range.first).trimEnd(' ', ',', ';', '-')
        }
        if (out != text) Log.d("StockPhrase", "$where trimmed [$text] to [$out]")
        return out
    }

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
