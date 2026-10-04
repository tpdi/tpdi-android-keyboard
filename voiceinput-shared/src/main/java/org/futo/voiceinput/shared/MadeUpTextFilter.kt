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
        if (isRepetitionLoop(text)) return true
        if (speechFrames == Int.MAX_VALUE) return false
        val wordCount = text.split(Regex("\\s+")).count { it.isNotEmpty() }
        val maxWords = (speechFrames * SECONDS_PER_VAD_FRAME * MAX_WORDS_PER_SECOND + SLACK_WORDS).toInt()
        return wordCount > maxWords
    }
}
