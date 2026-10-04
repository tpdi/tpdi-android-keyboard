package org.futo.voiceinput.shared

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
}
