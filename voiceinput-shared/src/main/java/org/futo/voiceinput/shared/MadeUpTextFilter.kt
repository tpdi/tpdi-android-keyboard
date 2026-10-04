package org.futo.voiceinput.shared

/**
 * The speech model sometimes invents text from near-silence or short unclear audio: one word or
 * sentence repeated over and over, or far more words than the speech heard could hold. These
 * checks recognise that output so it can be dropped.
 */
object MadeUpTextFilter {
    private const val MAX_WORDS_PER_SECOND = 4.5f
    private const val SLACK_WORDS = 4f
    private const val SECONDS_PER_VAD_FRAME = 0.03f

    // Phrases the model produces from silence or noise, evidently from video subtitles in its
    // training data. Dropped whenever they are the whole result.
    private val STOCK_PHRASES = setOf(
        "thanks for watching", "thank you for watching", "thanks for watching and see you next time",
        "please subscribe", "like and subscribe", "subscribe to my channel",
        "see you in the next video", "see you next time", "subtitles by the amaraorg community"
    )

    // Short phrases that are also real speech: only dropped when the audio held very little
    // speech (under SHORT_SPEECH_FRAMES VAD frames, about half a second).
    private val SHORT_PHRASES = setOf("thank you", "thanks", "bye", "you", "okay", "so")
    private const val SHORT_SPEECH_FRAMES = 15

    private fun normalized(text: String) =
        text.lowercase().replace(Regex("[^\\p{L}\\p{N}' ]"), " ").trim().replace(Regex("\\s+"), " ")

    fun isStockPhrase(text: String) = normalized(text) in STOCK_PHRASES

    /** Four or more words that are all the same word, or one sentence repeated three or more times. */
    fun isRepetitionLoop(text: String): Boolean {
        val words = text.lowercase().split(Regex("[^\\p{L}\\p{N}']+")).filter { it.isNotEmpty() }
        if (words.size >= 4 && words.toSet().size == 1) return true
        val sentences = text.lowercase().split(Regex("[.!?]+"))
            .map { it.replace(Regex("[^\\p{L}\\p{N}' ]"), "").trim() }
            .filter { it.isNotEmpty() }
        return sentences.size >= 3 && sentences.groupingBy { it }.eachCount().values.max() >= 3
    }

    /** A segment's result is made up if it loops, or has more words than [speechFrames] of speech could hold. */
    fun isMadeUpSegment(text: String, speechFrames: Int): Boolean {
        if (isRepetitionLoop(text) || isStockPhrase(text)) return true
        if (speechFrames < SHORT_SPEECH_FRAMES && normalized(text) in SHORT_PHRASES) return true
        if (speechFrames == Int.MAX_VALUE) return false
        val wordCount = text.split(Regex("\\s+")).count { it.isNotEmpty() }
        val maxWords = (speechFrames * SECONDS_PER_VAD_FRAME * MAX_WORDS_PER_SECOND + SLACK_WORDS).toInt()
        return wordCount > maxWords
    }
}
