package org.futo.voiceinput.shared

import android.util.Log
import kotlin.math.abs
import kotlin.math.max
import kotlin.math.sqrt

/**
 * Click-gesture detection. The "click" is a tongue click: the short, sharp "tsk" sound made by
 * pulling the tongue off the roof of the mouth, picked up by the microphone. It is a brief, sharp,
 * isolated transient -- very different from speech, which carries energy over much longer
 * stretches. (Clicking a pen or tapping the phone would also register, but a tongue click is what
 * this is meant for: hands-free, with no extra hardware.) Tracked independently of the
 * VAD/transcription pipeline; the audio never reaches Whisper for this. [AudioRecognizer] feeds
 * it every 100ms chunk and applies the [Result] it returns; it exists only when the click
 * gestures setting is on.
 *
 * Tongue-click times (ms) are collected; once CLICK_WINDOW_MS has passed since the last one, the
 * count decides whether it was a double click (Enter); three or more does nothing extra.
 */
class ClickGestureDetector {
    /** What [AudioRecognizer] should do after a chunk, in this order. */
    data class Result(val clearBuffer: Boolean, val finishSpeech: Boolean, val fireGesture: Int)

    class Analysis(val isClickCandidate: Boolean, val onsetSubIndices: List<Int>, val nSubs: Int)

    private val clickTimestamps = mutableListOf<Long>()
    private var lastClickAtMs = 0L

    // A click gesture waits here until speech spoken before it has been transcribed and
    // committed, so Enter can't land ahead of the text it follows.
    private var pendingGestureCount = 0

    // Consecutive 100ms chunks louder than the talking threshold; a click's ring-down is one or
    // two chunks, speech lasts longer.
    private var loudRun = 0

    // A real tongue click is followed by quiet; the opening consonants of a phrase look like clicks but
    // are followed by sustained speech. Onsets are cancelled if speech-level sound follows them
    // within CLICK_QUIET_AFTER_MS.
    private var onsetWindowStartMs = 0L
    private var postOnsetLoudChunks = 0

    // A deliberate tongue-click gesture has at least one clearly loud click; a pair of faint transients
    // (lip smacks, breath) after speech does not.
    private var groupMaxPeak = 0f

    // VAD speech frames (30ms each) seen since the last segment boundary; used to throw away
    // segments that are really just clicks plus silence.
    private var gestureSegmentSpeechFrames = 0

    // Click onsets are found at 6ms resolution inside each 100ms chunk (a chunk-level measure
    // merges fast double clicks and blurs a click's ring-down into the next chunk).
    private val subRms = FloatArray(16)
    private val subPeak = FloatArray(16)
    private var prevSubRms = 0f
    private var subsSinceOnset = 100
    private var totalSamplesRead = 0L

    fun onSamplesRead(n: Int) {
        totalSamplesRead += n
    }

    /**
     * Measured from the start of recording, not the buffer position: the buffer is cleared after
     * every segment and click gesture, which would blind detection for 0.6s each time.
     */
    fun startSoundPassed(): Boolean = totalSamplesRead > 16000 * 0.6

    fun onVadSpeechFrame() {
        gestureSegmentSpeechFrames++
    }

    /** At a segment boundary; true if the segment is just clicks plus silence and should be dropped. */
    fun segmentEnded(): Boolean {
        val tooLittleSpeech = gestureSegmentSpeechFrames < MIN_SEGMENT_SPEECH_FRAMES
        Log.d("ClickDetect", "segment end: speechFrames=$gestureSegmentSpeechFrames dropped=$tooLittleSpeech")
        gestureSegmentSpeechFrames = 0
        return tooLittleSpeech
    }

    fun cancelPending() {
        pendingGestureCount = 0
    }

    /** Finds click onsets in this chunk. Call before deciding whether the user has talked. */
    fun analyze(samples: ShortArray, nRead: Int, rms: Float, startSoundPassed: Boolean): Analysis {
        val peakAbs = (samples.maxOf { abs(it.toInt()) }).toFloat() / Short.MAX_VALUE.toFloat()
        val crestFactor = peakAbs / rms.coerceAtLeast(0.0001f)
        // Per-subframe energy, then onsets: a sharp rise to a peak well above the chunk's noise
        // floor, with no rise in the previous ~50ms (so a ring-down isn't recounted).
        val nSubs = (nRead / SUB).coerceAtMost(16)
        for (i in 0 until nSubs) {
            var sumSq = 0.0
            var pk = 0
            for (j in i * SUB until (i + 1) * SUB) {
                val v = samples[j].toInt()
                sumSq += v.toDouble() * v
                if (abs(v) > pk) pk = abs(v)
            }
            subRms[i] = (sqrt(sumSq / SUB) / Short.MAX_VALUE).toFloat()
            subPeak[i] = pk.toFloat() / Short.MAX_VALUE.toFloat()
        }
        val sortedSub = subRms.copyOf(nSubs).also { it.sort() }
        val floorRms = if (nSubs > 0) sortedSub[nSubs / 2].coerceAtLeast(0.002f) else 0.002f
        val onsetSubIndices = mutableListOf<Int>()
        if (startSoundPassed && rms < CLICK_RMS_CEILING) {
            for (i in 0 until nSubs) {
                subsSinceOnset++
                val before = if (i == 0) prevSubRms else subRms[i - 1]
                if (subPeak[i] > CLICK_PEAK_FLOOR && subRms[i] > 5f * floorRms &&
                    before < 0.5f * subRms[i] && subsSinceOnset >= 8) {
                    onsetSubIndices.add(i)
                    subsSinceOnset = 0
                }
            }
        } else {
            subsSinceOnset += nSubs
        }
        if (nSubs > 0) prevSubRms = subRms[nSubs - 1]
        val isClickCandidate = onsetSubIndices.isNotEmpty() || (startSoundPassed &&
                peakAbs > CLICK_PEAK_FLOOR &&
                rms < CLICK_RMS_CEILING &&
                crestFactor > CLICK_CREST_FACTOR_THRESHOLD)

        if (startSoundPassed && peakAbs > 0.05f) {
            // TEMPORARY: calibration logging, remove once thresholds are tuned against
            // real-device data. Logs any moderately loud chunk, not just ones that already pass
            // the thresholds, so we can see what a real click actually looks like.
            Log.d("ClickDetect", "peak=%.3f rms=%.4f crest=%.1f candidate=%b".format(peakAbs, rms, crestFactor, isClickCandidate))
        }
        return Analysis(isClickCandidate, onsetSubIndices, nSubs)
    }

    /**
     * Whether this chunk is loud enough, for long enough, to count as talking. A click's brief
     * energy must not flip hasTalked, which would send click-only audio to the model ("Thank
     * you", "Thanks for watching").
     */
    fun isSustainedLoud(startSoundPassed: Boolean, analysis: Analysis, rms: Float): Boolean {
        loudRun = if (startSoundPassed && !analysis.isClickCandidate && rms > 0.01) loudRun + 1 else 0
        return loudRun >= SUSTAINED_LOUD_CHUNKS
    }

    /** Click bookkeeping for this chunk, after hasTalked has been updated. */
    fun onChunk(
        analysis: Analysis,
        rms: Float,
        hasTalked: Boolean,
        isSegmentProcessing: Boolean
    ): Result {
        val now = System.currentTimeMillis()
        val onsetSubIndices = analysis.onsetSubIndices
        val nSubs = analysis.nSubs
        var clearBuffer = false
        var finishSpeech = false
        var fire = 0

        // Cancel recent onsets that turned out to be the start of speech.
        if (onsetWindowStartMs > 0L) {
            if (now - onsetWindowStartMs > CLICK_QUIET_AFTER_MS) {
                onsetWindowStartMs = 0L
                postOnsetLoudChunks = 0
            } else if (onsetSubIndices.isEmpty() && rms > CLICK_FOLLOWING_SPEECH_RMS) {
                postOnsetLoudChunks++
                if (postOnsetLoudChunks >= 2) {
                    val cutoff = onsetWindowStartMs - 50L
                    val before = clickTimestamps.size
                    clickTimestamps.removeAll { it >= cutoff }
                    lastClickAtMs = clickTimestamps.lastOrNull() ?: 0L
                    Log.d("ClickDetect", "dropped ${before - clickTimestamps.size} onset(s) followed by speech")
                    onsetWindowStartMs = 0L
                    postOnsetLoudChunks = 0
                }
            }
        }
        for (idx in onsetSubIndices) {
            val t = now - ((nSubs - idx) * SUB * 1000L / 16000L)
            // A key tap on the keyboard sounds like a click; ignore onsets near key presses.
            if (abs(t - ClickSuppression.lastKeyPressMs) < 400L) continue
            clickTimestamps.add(t)
            groupMaxPeak = max(groupMaxPeak, subPeak[idx])
            lastClickAtMs = t
            if (onsetWindowStartMs == 0L) {
                onsetWindowStartMs = t
                postOnsetLoudChunks = 0
            }
            Log.d("ClickDetect", "ONSET click #${clickTimestamps.size} sub=$idx")
        }

        if (clickTimestamps.isNotEmpty() && (now - lastClickAtMs) > CLICK_WINDOW_MS) {
            val count = clickTimestamps.size
            val tooSoft = groupMaxPeak < CLICK_GROUP_PEAK_MIN
            if (count >= 2 && tooSoft) {
                Log.d("ClickDetect", "dropped soft group: clicks=$count maxPeak=$groupMaxPeak")
            }
            clickTimestamps.clear()
            groupMaxPeak = 0f
            if (count >= 2 && !tooSoft) {
                pendingGestureCount = count.coerceAtMost(3)
                Log.d("ClickDetect", "gesture group closed: clicks=$count hasTalked=$hasTalked segmentProcessing=$isSegmentProcessing")
                // Clicks alone make Whisper hallucinate ("Thank you"); drop them unless speech
                // is still waiting in the buffer.
                if (!hasTalked) clearBuffer = true
            }
        }

        if (pendingGestureCount > 0) {
            if (hasTalked && !isSegmentProcessing) {
                // Speech before the click hasn't been sent for transcription yet; do it now.
                gestureSegmentSpeechFrames = 0
                finishSpeech = true
            } else if (!hasTalked && !isSegmentProcessing) {
                fire = pendingGestureCount
                pendingGestureCount = 0
                Log.d("ClickDetect", "gesture fired: $fire")
            }
        }
        return Result(clearBuffer, finishSpeech, fire)
    }

    private companion object {
        const val SUB = 100
        const val CLICK_PEAK_FLOOR = 0.04f
        const val CLICK_RMS_CEILING = 0.05f
        const val CLICK_CREST_FACTOR_THRESHOLD = 8.0f
        const val CLICK_WINDOW_MS = 1500L
        const val MIN_SEGMENT_SPEECH_FRAMES = 10
        const val CLICK_QUIET_AFTER_MS = 500L
        const val CLICK_GROUP_PEAK_MIN = 0.15f
        const val CLICK_FOLLOWING_SPEECH_RMS = 0.025f
        const val SUSTAINED_LOUD_CHUNKS = 3
    }
}
