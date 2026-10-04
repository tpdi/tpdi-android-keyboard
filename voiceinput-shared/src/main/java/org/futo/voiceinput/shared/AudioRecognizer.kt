package org.futo.voiceinput.shared

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.hardware.SensorPrivacyManager
import android.media.AudioDeviceInfo
import android.media.AudioFocusRequest
import android.media.AudioFormat
import android.media.AudioManager
import android.media.AudioRecord
import android.media.MediaRecorder
import android.media.MicrophoneDirection
import android.net.Uri
import android.os.Build
import android.provider.Settings
import android.util.Log
import androidx.lifecycle.LifecycleCoroutineScope
import com.konovalov.vad.Vad
import com.konovalov.vad.config.FrameSize
import com.konovalov.vad.config.Mode
import com.konovalov.vad.config.Model
import com.konovalov.vad.config.SampleRate
import com.konovalov.vad.models.VadModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.futo.voiceinput.shared.ggml.InferenceCancelledException
import org.futo.voiceinput.shared.ggml.InvalidModelException
import org.futo.voiceinput.shared.types.AudioRecognizerListener
import org.futo.voiceinput.shared.types.InferenceState
import org.futo.voiceinput.shared.types.Language
import org.futo.voiceinput.shared.types.MagnitudeState
import org.futo.voiceinput.shared.types.ModelInferenceCallback
import org.futo.voiceinput.shared.types.ModelLoader
import org.futo.voiceinput.shared.ui.MicrophoneDeviceState
import org.futo.voiceinput.shared.whisper.DecodingConfiguration
import org.futo.voiceinput.shared.whisper.ModelManager
import org.futo.voiceinput.shared.whisper.MultiModelRunConfiguration
import org.futo.voiceinput.shared.whisper.MultiModelRunner
import org.futo.voiceinput.shared.whisper.isBlankResult
import java.nio.FloatBuffer
import java.nio.ShortBuffer
import kotlin.math.min
import kotlin.math.pow
import kotlin.math.sqrt

private fun getRecordingDeviceKind(type: Int): String {
    return when (type) {
        AudioDeviceInfo.TYPE_BUILTIN_EARPIECE -> "BUILTIN"
        AudioDeviceInfo.TYPE_BUILTIN_MIC -> "BUILTIN"
        AudioDeviceInfo.TYPE_BLUETOOTH_SCO -> "BLUETOOTH_SCO"
        AudioDeviceInfo.TYPE_BLUETOOTH_A2DP -> "BLUETOOTH_A2DP"
        AudioDeviceInfo.TYPE_WIRED_HEADSET -> "WIRED_HEADSET"
        AudioDeviceInfo.TYPE_HDMI -> "HDMI"
        AudioDeviceInfo.TYPE_TELEPHONY -> "TELEPHONY"
        AudioDeviceInfo.TYPE_DOCK -> "DOCK"
        AudioDeviceInfo.TYPE_USB_ACCESSORY -> "USB_ACCESSORY"
        AudioDeviceInfo.TYPE_USB_DEVICE -> "USB_DEVICE"
        AudioDeviceInfo.TYPE_USB_HEADSET -> "USB_HEADSET"
        AudioDeviceInfo.TYPE_FM_TUNER -> "FM_TUNER"
        AudioDeviceInfo.TYPE_TV_TUNER -> "TV_TUNER"
        AudioDeviceInfo.TYPE_LINE_ANALOG -> "LINE_ANALOG"
        AudioDeviceInfo.TYPE_LINE_DIGITAL -> "LINE_DIGITAL"
        AudioDeviceInfo.TYPE_IP -> "IP"
        AudioDeviceInfo.TYPE_BUS -> "BUS"
        AudioDeviceInfo.TYPE_REMOTE_SUBMIX -> "REMOTE_SUBMIX"
        AudioDeviceInfo.TYPE_BLE_HEADSET -> "BLE_HEADSET"
        AudioDeviceInfo.TYPE_HDMI_ARC -> "HDMI_ARC"
        AudioDeviceInfo.TYPE_HDMI_EARC -> "HDMI_EARC"
        AudioDeviceInfo.TYPE_DOCK_ANALOG -> "DOCK_ANALOG"
        else -> "unknown@${type}"
    }
}

data class RecordingSettings(
    val preferBluetoothMic: Boolean,
    val requestAudioFocus: Boolean,
    val canExpandSpace: Boolean,
    val useVADAutoStop: Boolean,
    val useSegmentedResults: Boolean = false,
    val segmentPauseMs: Int = 600,
    val filterMadeUpText: Boolean = false,
    val useClickGestures: Boolean = false
)

data class AudioRecognizerSettings(
    val modelRunConfiguration: MultiModelRunConfiguration,
    val decodingConfiguration: DecodingConfiguration,
    val recordingConfiguration: RecordingSettings
)

class ModelDoesNotExistException(val models: List<ModelLoader>) : Throwable()

class AudioRecognizer(
    private val context: Context,
    private val lifecycleScope: LifecycleCoroutineScope,
    modelManager: ModelManager,
    private val listener: AudioRecognizerListener,
    private val settings: AudioRecognizerSettings
) {
    private var isRecording = false
    private var recorder: AudioRecord? = null

    private val modelRunner = MultiModelRunner(modelManager)

    private val canExpandSpace = settings.recordingConfiguration.canExpandSpace
    private val useVAD = settings.recordingConfiguration.useVADAutoStop
    private val useSegmentedResults = settings.recordingConfiguration.useSegmentedResults
    // VAD runs in 480-sample (30ms @ 16kHz) frames; convert the configured ms to a frame count.
    private val segmentPauseFrames = (settings.recordingConfiguration.segmentPauseMs / 30).coerceAtLeast(1)

    private val filterMadeUpText = settings.recordingConfiguration.filterMadeUpText

    // Whether any speech has been heard since the last segment boundary, and how many 30ms VAD
    // speech frames the current segment holds. Used only when filterMadeUpText is on.
    @Volatile
    private var bufferHasSpeech = false
    private var segmentSpeechFrames = 0
    private val useClickGestures = settings.recordingConfiguration.useClickGestures

    // A click gesture waits here until speech spoken before it has been transcribed and
    // committed, so Enter can't land ahead of the text it follows.
    private var pendingGestureCount = 0

    private var floatSamples: FloatBuffer = FloatBuffer.allocate(16000 * 30)
    private var recorderJob: Job? = null
    private var modelJob: Job? = null
    private var loadModelJob: Job? = null
    private var segmentJob: Job? = null
    private var isSegmentProcessing = false

    private var focusRequest: AudioFocusRequest? = null

    private var communicationDevice = "unknown"

    private fun focusAudio() {
        unfocusAudio()

        if(!settings.recordingConfiguration.requestAudioFocus) return

        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                focusRequest =
                    AudioFocusRequest.Builder(AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_EXCLUSIVE)
                        .build()
                audioManager.requestAudioFocus(focusRequest!!)
            }
        }catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun unfocusAudio() {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                if (focusRequest != null) {
                    audioManager.abandonAudioFocusRequest(focusRequest!!)
                }
                focusRequest = null
            }
        }catch(e: Exception) {
            e.printStackTrace()
        }
    }

    private fun isBluetoothAvailable(): Boolean {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val devices = audioManager.availableCommunicationDevices

                return devices.firstOrNull {
                    it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO
                } != null
            }
        } catch(_: Exception) {}

        return false
    }

    private fun setCommunicationDevice(preferBluetoothMic: Boolean): Pair<Boolean, String> {
        try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
                val devices = audioManager.availableCommunicationDevices
                val tgtDevice =
                    devices.firstOrNull { preferBluetoothMic && it.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO } ?:
                    devices.firstOrNull { it.type == AudioDeviceInfo.TYPE_BUILTIN_MIC } ?:
                    devices.firstOrNull { it.type != AudioDeviceInfo.TYPE_BLUETOOTH_SCO  } ?:
                    devices.first()

                if (!audioManager.setCommunicationDevice(tgtDevice)) {
                    audioManager.clearCommunicationDevice()
                    return Pair(false, "")
                } else {
                    return Pair(tgtDevice.type == AudioDeviceInfo.TYPE_BLUETOOTH_SCO, tgtDevice.productName.toString())
                }
            }
        } catch(_: Exception) {}
        return Pair(false, "")
    }

    private fun clearCommunicationDevice() {
        val audioManager = context.getSystemService(Context.AUDIO_SERVICE) as AudioManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            audioManager.clearCommunicationDevice()
        }
    }

    @Throws(ModelDoesNotExistException::class)
    private fun verifyModelsExist() {
        val modelsThatDoNotExist = mutableListOf<ModelLoader>()

        if (!settings.modelRunConfiguration.primaryModel.exists(context)) {
            modelsThatDoNotExist.add(settings.modelRunConfiguration.primaryModel)
        }

        for (model in settings.modelRunConfiguration.languageSpecificModels.values) {
            if (!model.exists(context)) {
                modelsThatDoNotExist.add(model)
            }
        }

        if (modelsThatDoNotExist.isNotEmpty()) {
            throw ModelDoesNotExistException(modelsThatDoNotExist)
        }
    }

    init {
        verifyModelsExist()
    }

    fun reset() {
        recorder?.stop()
        recorderJob?.cancel()

        recorder?.release()
        recorder = null

        modelJob?.cancel()
        segmentJob?.cancel()
        isSegmentProcessing = false
        pendingGestureCount = 0
        isRecording = false

        modelRunner.cancelAll()

        unfocusAudio()

        clearCommunicationDevice()
    }

    fun finish() {
        if(!isRecording) return
        onFinishRecording()
    }

    /**
     * Finalizes whatever audio has been captured since the last segment (or
     * the start of recording) as its own independent decode, without
     * stopping the recorder. Unlike [finish]/[onFinishRecording], recording
     * continues immediately; this only snapshots-and-clears the sample
     * buffer so the next segment starts clean.
     */
    private fun finishSegment(speechFrames: Int = Int.MAX_VALUE) {
        if (!isRecording || isSegmentProcessing) return

        val segmentSamples = floatSamples.array().sliceArray(0 until floatSamples.position())
        floatSamples.clear()
        bufferHasSpeech = false

        if (segmentSamples.isEmpty()) return

        isSegmentProcessing = true
        listener.segmentStarted()
        segmentJob = lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                runSegmentModel(segmentSamples, speechFrames)
            }
        }
    }

    fun cancel() {
        reset()
        listener.cancelled()
    }

    fun openPermissionSettings() {
        val packageName = context.packageName
        val micPermissionRequester = Intent()
        micPermissionRequester.setClassName(context, "org.futo.inputmethod.latin.MicPermissionActivity")
        micPermissionRequester.setFlags(
            Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
        )
        context.startActivity(micPermissionRequester)

        cancel()
    }

    fun start() {
        listener.loading()

        if (context.checkSelfPermission(Manifest.permission.RECORD_AUDIO) != PackageManager.PERMISSION_GRANTED) {
            requestPermission()
        } else {
            startRecording()
        }
    }

    private fun requestPermission() {
        listener.needPermission { wasGranted ->
            if(wasGranted) {
                startRecording()
            }
        }
    }


    @Throws(SecurityException::class)
    private fun createAudioRecorder(): AudioRecord {
        val recorder = AudioRecord(
            MediaRecorder.AudioSource.VOICE_RECOGNITION,
            16000,
            AudioFormat.CHANNEL_IN_MONO,
            AudioFormat.ENCODING_PCM_16BIT,
            16000 * 2 * 5
        )

        this.recorder = recorder

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            recorder.setPreferredMicrophoneDirection(MicrophoneDirection.MIC_DIRECTION_TOWARDS_USER)
        }

        return recorder
    }

    private suspend fun preloadModels() {
        modelRunner.preload(settings.modelRunConfiguration)
    }

    private fun expandSpaceIfAllowed(): Boolean {
        if(canExpandSpace) {
            // Allocate an extra 30 seconds
            val newSampleBuffer = FloatBuffer.allocate(floatSamples.capacity() + 16000 * 30)
            //Log.d("AudioRecognizer", "Allocating extra space: ${floatSamples.capacity() / 16000} -> ${newSampleBuffer.capacity() / 16000}")
            newSampleBuffer.put(floatSamples.array(), 0, floatSamples.capacity() - floatSamples.remaining())
            floatSamples = newSampleBuffer
            return true
        }
        return false
    }

    private suspend fun recordingJob(recorder: AudioRecord, vad: VadModel?) {
        var hasTalked = false
        var anyNoiseAtAll = false

        val canMicBeBlocked = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            (context.getSystemService(SensorPrivacyManager::class.java) as SensorPrivacyManager).supportsSensorToggle(
                SensorPrivacyManager.Sensors.MICROPHONE
            )
        } else {
            false
        }
        var isMicBlocked = false

        val vadSampleBuffer = ShortBuffer.allocate(480)
        var numConsecutiveNonSpeech = 0
        var numConsecutiveSpeech = 0

        // Click-gesture detection: a click is a brief, sharp, isolated transient -- very
        // different from speech, which carries energy over much longer stretches. Tracked
        // independently of the VAD/transcription pipeline entirely; the audio never reaches
        // Whisper for this. clickTimestamps holds recent click times (ms); once CLICK_WINDOW_MS
        // has elapsed since the first one, the count decides whether it was a double (Enter); three or more does nothing.
        val clickTimestamps = mutableListOf<Long>()
        var lastClickAtMs = 0L
        val CLICK_PEAK_FLOOR = 0.04f
        val CLICK_RMS_CEILING = 0.05f
        val CLICK_CREST_FACTOR_THRESHOLD = 8.0f
        val CLICK_COOLDOWN_MS = 80L
        val CLICK_WINDOW_MS = 1500L
        val MIN_SEGMENT_SPEECH_FRAMES = 10

        // Consecutive 100ms chunks louder than the talking threshold; a click's ring-down is
        // one or two chunks, speech lasts longer.
        var loudRun = 0
        // A real click is followed by quiet; the opening consonants of a phrase look like clicks
        // but are followed by sustained speech. Onsets are cancelled if speech-level sound
        // follows them within CLICK_QUIET_AFTER_MS.
        var onsetWindowStartMs = 0L
        var postOnsetLoudChunks = 0
        val CLICK_QUIET_AFTER_MS = 500L
        val CLICK_FOLLOWING_SPEECH_RMS = 0.025f
        // VAD speech frames (30ms each) seen since the last segment boundary; used to throw away
        // segments that are really just clicks plus silence.
        var gestureSegmentSpeechFrames = 0

        // Click onsets are found at 6ms resolution inside each 100ms chunk (a chunk-level measure
        // merges fast double clicks and blurs a click's ring-down into the next chunk).
        val SUB = 100
        val subRms = FloatArray(16)
        val subPeak = FloatArray(16)
        var prevSubRms = 0f
        var subsSinceOnset = 100
        var totalSamplesRead = 0L

        val samples = ShortArray(1600)

        while (isRecording) {
            yield()
            val nRead = recorder.read(samples, 0, 1600, AudioRecord.READ_BLOCKING)
            if (nRead <= 0) break
            yield()

            var isRunningOutOfSpace = (floatSamples.remaining() < nRead.coerceAtLeast(1600)) && !expandSpaceIfAllowed()

            val hasNotTalkedRecently = hasTalked && (numConsecutiveNonSpeech > 66) && useVAD
            if (isRunningOutOfSpace || hasNotTalkedRecently) {
                yield()
                withContext(Dispatchers.Main) {
                    finish()
                }
                return
            }

            val shouldFinalizeSegment = useSegmentedResults && useVAD && hasTalked &&
                    (numConsecutiveNonSpeech > segmentPauseFrames) && !isSegmentProcessing
            if (shouldFinalizeSegment) {
                numConsecutiveNonSpeech = 0
                numConsecutiveSpeech = 0
                hasTalked = false
                val framesThisSegment = segmentSpeechFrames
                segmentSpeechFrames = 0
                val tooLittleSpeech = useClickGestures && gestureSegmentSpeechFrames < MIN_SEGMENT_SPEECH_FRAMES
                android.util.Log.d("ClickDetect", "segment end: speechFrames=$gestureSegmentSpeechFrames dropped=$tooLittleSpeech")
                gestureSegmentSpeechFrames = 0
                if (tooLittleSpeech) {
                    floatSamples.clear()
                    bufferHasSpeech = false
                } else {
                    yield()
                    withContext(Dispatchers.Main) {
                        finishSegment(framesThisSegment)
                    }
                }
            }

            // Run VAD
            if(useVAD && vad != null) {
                var remainingSamples = nRead
                var offset = 0
                while (remainingSamples > 0) {
                    if (!vadSampleBuffer.hasRemaining()) {
                        val isSpeech = vad.isSpeech(vadSampleBuffer.array())
                        vadSampleBuffer.clear()
                        vadSampleBuffer.rewind()

                        if (!isSpeech) {
                            numConsecutiveNonSpeech++
                            numConsecutiveSpeech = 0
                        } else {
                            numConsecutiveNonSpeech = 0
                            numConsecutiveSpeech++
                            segmentSpeechFrames++
                            gestureSegmentSpeechFrames++
                        }
                    }

                    val samplesToRead = min(min(remainingSamples, 480), vadSampleBuffer.remaining())
                    for (i in 0 until samplesToRead) {
                        vadSampleBuffer.put(
                            samples[offset]
                        )
                        offset += 1
                        remainingSamples -= 1
                    }
                }
            }

            floatSamples.put(samples.sliceArray(0 until nRead).map { it.toFloat() / Short.MAX_VALUE.toFloat() }.toFloatArray())

            // Don't set hasTalked if the start sound may still be playing, otherwise on some
            // devices the rms just explodes and `hasTalked` is always true
            totalSamplesRead += nRead
            // With click gestures on, measured from the start of recording, not the buffer
            // position: the buffer is cleared after every segment and click gesture, which would
            // blind detection for 0.6s each time.
            val startSoundPassed = if (useClickGestures) (totalSamplesRead > 16000 * 0.6)
                                   else (floatSamples.position() > 16000 * 0.6)
            if (!startSoundPassed) {
                numConsecutiveSpeech = 0
                numConsecutiveNonSpeech = 0
            }

            val rms = sqrt(samples.sumOf { (it.toFloat() / Short.MAX_VALUE.toFloat()).pow(2).toDouble() } / samples.size).toFloat()

            // Evaluate click-ness first so a click's brief energy can't flip hasTalked, which
            // would send click-only audio to the model ("Thank you", "Thanks for watching").
            val peakAbs = (samples.maxOf { kotlin.math.abs(it.toInt()) }).toFloat() / Short.MAX_VALUE.toFloat()
            val crestFactor = peakAbs / rms.coerceAtLeast(0.0001f)
            // Per-subframe energy, then onsets: a sharp rise to a peak well above the chunk's
            // noise floor, with no rise in the previous ~50ms (so a ring-down isn't recounted).
            val nSubs = (nRead / SUB).coerceAtMost(16)
            for (i in 0 until nSubs) {
                var sumSq = 0.0
                var pk = 0
                for (j in i * SUB until (i + 1) * SUB) {
                    val v = samples[j].toInt()
                    sumSq += v.toDouble() * v
                    if (kotlin.math.abs(v) > pk) pk = kotlin.math.abs(v)
                }
                subRms[i] = (sqrt(sumSq / SUB) / Short.MAX_VALUE).toFloat()
                subPeak[i] = pk.toFloat() / Short.MAX_VALUE.toFloat()
            }
            val sortedSub = subRms.copyOf(nSubs).also { it.sort() }
            val floorRms = if (nSubs > 0) sortedSub[nSubs / 2].coerceAtLeast(0.002f) else 0.002f
            val onsetSubIndices = mutableListOf<Int>()
            if (useClickGestures && startSoundPassed && rms < CLICK_RMS_CEILING) {
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
            val isClickCandidate = onsetSubIndices.isNotEmpty() || (useClickGestures && startSoundPassed &&
                    peakAbs > CLICK_PEAK_FLOOR &&
                    rms < CLICK_RMS_CEILING &&
                    crestFactor > CLICK_CREST_FACTOR_THRESHOLD)
            if (isClickCandidate) numConsecutiveSpeech = 0

            loudRun = if (startSoundPassed && !isClickCandidate && rms > 0.01) loudRun + 1 else 0
            val sustainedLoudRequired = if (useClickGestures) 3 else 1
            if (startSoundPassed && ((loudRun >= sustainedLoudRequired) || (numConsecutiveSpeech > 8))) {
                hasTalked = true
            }
            if (hasTalked) bufferHasSpeech = true

            if (useClickGestures && startSoundPassed) {
                // TEMPORARY: calibration logging, remove once thresholds are tuned against
                // real-device data. Logs any moderately loud chunk, not just ones that already
                // pass the thresholds, so we can see what a real click actually looks like.
                if (peakAbs > 0.05f) {
                    android.util.Log.d(
                        "ClickDetect",
                        "peak=%.3f rms=%.4f crest=%.1f candidate=%b".format(peakAbs, rms, crestFactor, isClickCandidate)
                    )
                }

                val now = System.currentTimeMillis()
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
                            android.util.Log.d("ClickDetect", "dropped ${before - clickTimestamps.size} onset(s) followed by speech")
                            onsetWindowStartMs = 0L
                            postOnsetLoudChunks = 0
                        }
                    }
                }
                for (idx in onsetSubIndices) {
                    val t = now - ((nSubs - idx) * SUB * 1000L / 16000L)
                    // A key tap on the keyboard sounds like a click; ignore onsets near key presses.
                    if (kotlin.math.abs(t - ClickSuppression.lastKeyPressMs) < 400L) continue
                    clickTimestamps.add(t)
                    lastClickAtMs = t
                    if (onsetWindowStartMs == 0L) {
                        onsetWindowStartMs = t
                        postOnsetLoudChunks = 0
                    }
                    android.util.Log.d("ClickDetect", "ONSET click #${clickTimestamps.size} sub=$idx")
                }

                if (clickTimestamps.isNotEmpty() && (now - lastClickAtMs) > CLICK_WINDOW_MS) {
                    val count = clickTimestamps.size
                    clickTimestamps.clear()
                    if (count >= 2) {
                        pendingGestureCount = count.coerceAtMost(3)
                        android.util.Log.d("ClickDetect", "gesture group closed: clicks=$count hasTalked=$hasTalked segmentProcessing=$isSegmentProcessing")
                        // Clicks alone make Whisper hallucinate ("Thank you"); drop them unless
                        // speech is still waiting in the buffer.
                        if (!hasTalked) {
                            floatSamples.clear()
                        }
                    }
                }

                if (pendingGestureCount > 0) {
                    if (hasTalked && !isSegmentProcessing) {
                        // Speech before the click hasn't been sent for transcription yet; do it now.
                        hasTalked = false
                        numConsecutiveSpeech = 0
                        numConsecutiveNonSpeech = 0
                        gestureSegmentSpeechFrames = 0
                        yield()
                        withContext(Dispatchers.Main) {
                            finishSegment()
                        }
                    } else if (!hasTalked && !isSegmentProcessing) {
                        val reportedCount = pendingGestureCount
                        pendingGestureCount = 0
                        android.util.Log.d("ClickDetect", "gesture fired: $reportedCount")
                        yield()
                        withContext(Dispatchers.Main) {
                            listener.clickGesture(reportedCount)
                        }
                    }
                }
            }


            if (rms > 0.0001) {
                anyNoiseAtAll = true
                isMicBlocked = false
            }

            // Check if mic is blocked
            val blockCheckTimePassed = (floatSamples.position() > 2 * 16000) // two seconds
            if (!anyNoiseAtAll && canMicBeBlocked && blockCheckTimePassed) {
                isMicBlocked = true
            }

            val magnitude = (1.0f - 0.1f.pow(24.0f * rms))

            val state = if (hasTalked) {
                MagnitudeState.TALKING
            } else if (isMicBlocked) {
                MagnitudeState.MIC_MAY_BE_BLOCKED
            } else {
                MagnitudeState.NOT_TALKED_YET
            }

            yield()
            withContext(Dispatchers.Main) {
                listener.updateMagnitude(magnitude, state)
            }

            // Skip ahead as much as possible, in case we are behind (taking more than
            // 100ms to process 100ms)
            while (true) {
                yield()
                val nRead2 = recorder.read(
                    samples, 0, 1600, AudioRecord.READ_NON_BLOCKING
                )
                if (nRead2 > 0) {
                    if (floatSamples.remaining() < nRead2 && !expandSpaceIfAllowed()) {
                        yield()
                        withContext(Dispatchers.Main) {
                            finish()
                        }
                        break
                    }
                    floatSamples.put(samples.sliceArray(0 until nRead2).map { it.toFloat() / Short.MAX_VALUE.toFloat() }.toFloatArray())
                } else {
                    break
                }
            }
        }
        println("isRecording loop exited")
    }

    private fun createVad(): VadModel {
        return Vad.builder().setModel(Model.WEB_RTC_GMM).setMode(Mode.VERY_AGGRESSIVE)
            .setFrameSize(FrameSize.FRAME_SIZE_480).setSampleRate(SampleRate.SAMPLE_RATE_16K)
            .setSpeechDurationMs(150).setSilenceDurationMs(300).build()
    }

    @Throws(SecurityException::class)
    private fun createRecorderAndJob(preferBluetoothMic: Boolean): MicrophoneDeviceState {
        isRecording = false
        recorder?.stop()

        val bluetoothInfo = setCommunicationDevice(preferBluetoothMic)

        val task = {
            recorder?.release()

            val recorder = createAudioRecorder()

            recorder.startRecording()
            this.recorder = recorder

            isRecording = true

            recorderJob = lifecycleScope.launch {
                withContext(Dispatchers.Default) {
                    if(useVAD) {
                        createVad().use { vad ->
                            recordingJob(recorder, vad)
                        }
                    } else {
                        recordingJob(recorder, null)
                    }
                }
            }
        }

        if(recorderJob != null) {
            lifecycleScope.launch {
                recorderJob?.cancelAndJoin()
                task()
            }
        } else {
            task()
        }


        return MicrophoneDeviceState(
            bluetoothAvailable = bluetoothInfo.first || isBluetoothAvailable(),
            bluetoothActive = bluetoothInfo.first,
            deviceName = bluetoothInfo.second,
            bluetoothPreferredByUser = settings.recordingConfiguration.preferBluetoothMic,
            setBluetooth = {
                listener.recordingStarted(createRecorderAndJob(it))
            }
        )
    }

    private fun startRecording() {
        val device = try {
            createRecorderAndJob(settings.recordingConfiguration.preferBluetoothMic)
        } catch (e: SecurityException) {
            // It's possible we may have lost permission, so let's just ask for permission again
            clearCommunicationDevice()
            requestPermission()
            return
        }

        focusAudio()

        listener.recordingStarted(device)

        loadModelJob = lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                try {
                    preloadModels()
                } catch(_: InvalidModelException) {
                    withContext(Dispatchers.Main) {
                        reset()
                        listener.modelLoadingFailed()
                    }
                }
            }
        }
    }

    private val runnerCallback: ModelInferenceCallback = object : ModelInferenceCallback {
        override fun updateStatus(state: InferenceState) {
            listener.decodingStatus(state)
        }

        override fun languageDetected(language: Language) {
            listener.languageDetected(language)
        }

        override fun partialResult(string: String) {
            if(isBlankResult(string)) return
            listener.partialResult(string)
        }
    }

    private suspend fun runSegmentModel(segmentSamples: FloatArray, speechFrames: Int) {
        loadModelJob?.let {
            if (it.isActive) it.join()
        }

        yield()
        val outputText = try {
            modelRunner.run(
                segmentSamples,
                settings.modelRunConfiguration,
                settings.decodingConfiguration,
                runnerCallback
            ).trim()
        } catch (e: InferenceCancelledException) {
            isSegmentProcessing = false
            return
        }

        // With click gestures on, stays true until the result has been handed to the listener,
        // so a pending click gesture can't be posted ahead of the text it follows.
        if (!useClickGestures) isSegmentProcessing = false

        var text = when {
            isBlankResult(outputText) -> ""
            else -> outputText
        }

        if (filterMadeUpText && MadeUpTextFilter.isMadeUpSegment(text, speechFrames)) text = ""

        if (text.isNotEmpty() || useClickGestures) {
            yield()
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    yield()
                    if (text.isNotEmpty()) listener.segmentResult(text)
                    if (useClickGestures) isSegmentProcessing = false
                }
            }
        }
    }

    private suspend fun runModel() {
        loadModelJob?.let {
            if (it.isActive) {
                println("Model was not finished loading...")
                it.join()
            }
        }

        // Don't let the final decode race a still-in-flight segment decode on the same model.
        segmentJob?.let {
            if (it.isActive) it.join()
        }

        // With segmented results the buffer holds only what came after the last segment; if no
        // speech was heard in it, skip the decode rather than let the model make something up.
        if (filterMadeUpText && useSegmentedResults && !bufferHasSpeech) {
            yield()
            lifecycleScope.launch {
                withContext(Dispatchers.Main) {
                    yield()
                    listener.finished("")
                }
            }
            return
        }

        val floatArray = floatSamples.array().sliceArray(0 until floatSamples.position())

        yield()
        val outputText = try {
             modelRunner.run(
                floatArray,
                settings.modelRunConfiguration,
                settings.decodingConfiguration,
                runnerCallback
            ).trim()
        }catch(e: InferenceCancelledException) {
            yield()
            return
        }

        var text = when {
            isBlankResult(outputText) -> ""
            else -> outputText
        }

        if (filterMadeUpText && MadeUpTextFilter.isRepetitionLoop(text)) text = ""

        yield()
        lifecycleScope.launch {
            withContext(Dispatchers.Main) {
                yield()
                listener.finished(text)
            }
        }
    }

    private fun onFinishRecording() {
        recorderJob?.cancel()

        if (!isRecording) {
            throw IllegalStateException("Should not call onFinishRecording when not recording")
        }

        isRecording = false
        recorder?.stop()

        listener.processing()

        modelJob = lifecycleScope.launch {
            withContext(Dispatchers.Default) {
                runModel()
            }
        }
    }
}