package org.futo.inputmethod.latin.uix.actions

import android.content.Intent
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.RowScope
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.Dp
import org.futo.inputmethod.latin.uix.ActionBarHeight
import org.futo.inputmethod.latin.uix.TypedTextTap
import org.futo.inputmethod.latin.uix.VOICE_INPUT_OVER_KEYBOARD
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.traversalIndex
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.ANIMATE_BUBBLE
import org.futo.inputmethod.latin.uix.AUDIO_FOCUS
import org.futo.inputmethod.latin.uix.Action
import org.futo.inputmethod.latin.uix.ActionWindow
import org.futo.inputmethod.latin.uix.CAN_EXPAND_SPACE
import org.futo.inputmethod.latin.uix.CloseResult
import org.futo.inputmethod.latin.uix.DISALLOW_SYMBOLS
import org.futo.inputmethod.latin.uix.ENABLE_SOUND
import org.futo.inputmethod.latin.uix.KeyboardManagerForAction
import org.futo.inputmethod.latin.uix.PREFER_BLUETOOTH
import org.futo.inputmethod.latin.uix.PersistentActionState
import org.futo.inputmethod.latin.uix.ResourceHelper
import org.futo.inputmethod.latin.uix.USE_PERSONAL_DICT
import org.futo.inputmethod.latin.uix.USE_VAD_AUTOSTOP
import org.futo.inputmethod.latin.uix.VERBOSE_PROGRESS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_ACTION_BUTTONS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_CLICK_GESTURES
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SEGMENTED_RESULTS
import org.futo.inputmethod.latin.uix.VOICE_INPUT_SEGMENT_PAUSE_MS
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.setSetting
import org.futo.inputmethod.latin.uix.settings.SettingsActivity
import org.futo.inputmethod.latin.uix.utils.ModelOutputSanitizer
import org.futo.inputmethod.latin.xlm.UserDictionaryObserver
import org.futo.inputmethod.updates.openURI
import org.futo.voiceinput.shared.ModelDoesNotExistException
import org.futo.voiceinput.shared.RecognizerView
import org.futo.voiceinput.shared.RecognizerViewListener
import org.futo.voiceinput.shared.RecognizerViewSettings
import org.futo.voiceinput.shared.RecordingSettings
import org.futo.voiceinput.shared.SoundPlayer
import org.futo.voiceinput.shared.types.Language
import org.futo.voiceinput.shared.types.ModelLoader
import org.futo.voiceinput.shared.types.getLanguageFromWhisperString
import org.futo.voiceinput.shared.ui.MicrophoneDeviceState
import org.futo.voiceinput.shared.whisper.DecodingConfiguration
import org.futo.voiceinput.shared.whisper.ModelManager
import org.futo.voiceinput.shared.whisper.MultiModelRunConfiguration
import java.util.Locale

val SystemVoiceInputAction = Action(
    icon = R.drawable.mic_fill,
    name = R.string.action_system_voice_input_title,
    simplePressImpl = { it, _ ->
        it.triggerSystemVoiceInput()
    },
    persistentState = null,
    windowImpl = null,
    shownInEditor = false
)


@Composable
fun NoModelInstalled(locale: Locale) {
    val context = LocalContext.current
    Box(modifier = Modifier
        .fillMaxSize()
        .clickable(
            enabled = true,
            onClickLabel = null,
            onClick = {
                context.openURI("https://keyboard.futo.tech/voice-input-models", true)
            },
            role = null,
            indication = null,
            interactionSource = remember { MutableInteractionSource() })) {
        Text(
            stringResource(
                R.string.action_voice_input_no_model_for_language_x_installed,
                locale.getDisplayName(locale)
            ), modifier = Modifier
                .align(Alignment.Center)
                .padding(8.dp), textAlign = TextAlign.Center)
    }
}

class VoiceInputPersistentState(val manager: KeyboardManagerForAction) : PersistentActionState {
    val modelManager = ModelManager(manager.getContext())
    val soundPlayer = SoundPlayer(manager.getContext())
    val userDictionaryObserver = UserDictionaryObserver(manager.getContext())

    override suspend fun cleanUp() {
        modelManager.cleanUp()
    }

    override fun close() {
        runBlocking { modelManager.cleanUp() }
        userDictionaryObserver.unregister()
    }
}

private class VoiceInputActionWindow(
    val manager: KeyboardManagerForAction, val state: VoiceInputPersistentState,
    val model: ModelLoader, val locales: List<Locale>
) : ActionWindow(), RecognizerViewListener {
    val context = manager.getContext()

    private var shouldPlaySounds: Boolean = false
    private var useActionButtons: Boolean = false
    private fun loadSettings(): RecognizerViewSettings {
        val enableSound = context.getSetting(ENABLE_SOUND)
        val verboseFeedback = false//context.getSetting(VERBOSE_PROGRESS)
        val disallowSymbols = context.getSetting(DISALLOW_SYMBOLS)
        val useBluetoothAudio = context.getSetting(PREFER_BLUETOOTH)
        val requestAudioFocus = context.getSetting(AUDIO_FOCUS)
        val canExpandSpace = context.getSetting(CAN_EXPAND_SPACE)
        val useVAD = context.getSetting(USE_VAD_AUTOSTOP)
        val useSegmentedResults = context.getSetting(VOICE_INPUT_SEGMENTED_RESULTS)
        val segmentPauseMs = context.getSetting(VOICE_INPUT_SEGMENT_PAUSE_MS)
        val useClickGestures = context.getSetting(VOICE_INPUT_CLICK_GESTURES)
        useActionButtons = context.getSetting(VOICE_INPUT_ACTION_BUTTONS)
        val usePersonalDict = context.getSetting(USE_PERSONAL_DICT)
        val animateBubble = context.getSetting(ANIMATE_BUBBLE)

        val primaryModel = model
        val languageSpecificModels = mutableMapOf<Language, ModelLoader>()
        val allowedLanguages = locales.mapNotNull { getLanguageFromWhisperString(it.language) }.toSet()
        val glossary = if(usePersonalDict) {
            state.userDictionaryObserver.getWords(locales).filter { it.shortcut.isNullOrEmpty() }.map { it.word }
        } else {
            emptyList()
        }

        shouldPlaySounds = enableSound

        return RecognizerViewSettings(
            shouldShowInlinePartialResult = false,
            shouldShowVerboseFeedback = verboseFeedback,
            shouldAnimateBubble = animateBubble,
            modelRunConfiguration = MultiModelRunConfiguration(
                primaryModel = primaryModel,
                languageSpecificModels = languageSpecificModels
            ),
            decodingConfiguration = DecodingConfiguration(
                glossary = glossary,
                languages = allowedLanguages,
                suppressSymbols = disallowSymbols
            ),
            recordingConfiguration = RecordingSettings(
                preferBluetoothMic = useBluetoothAudio,
                requestAudioFocus = requestAudioFocus,
                canExpandSpace = canExpandSpace,
                useVADAutoStop = useVAD,
                useSegmentedResults = useSegmentedResults,
                segmentPauseMs = segmentPauseMs,
                useClickGestures = useClickGestures
            )
        )
    }

    private var recognizerView: MutableState<RecognizerView?> = mutableStateOf(null)
    private var modelException: MutableState<ModelDoesNotExistException?> = mutableStateOf(null)

    private val initJob = manager.getLifecycleScope().launch(Dispatchers.Default) {
        yield()
        val settings = loadSettings()

        yield()
        val recognizerView = try {
            RecognizerView(
                context = manager.getContext(),
                listener = this@VoiceInputActionWindow,
                settings = settings,
                lifecycleScope = manager.getLifecycleScope(),
                modelManager = state.modelManager
            )
        } catch(e: ModelDoesNotExistException) {
            modelException.value = e
            return@launch
        }

        this@VoiceInputActionWindow.recognizerView.value = recognizerView

        //yield()
        recognizerView.reset()

        //yield()
        recognizerView.start()
    }

    private val inlineMode = context.getSetting(VOICE_INPUT_OVER_KEYBOARD)

    private fun newTransaction() =
        if (inlineMode) manager.createUnroutedInputTransaction() else manager.createInputTransaction()

    private var inputTransaction = newTransaction()

    // Text of what voice input itself has committed (segments, manual Enters), most recent
    // last -- lets Undo remove just our own output, one unit at a time, repeatable. Stores the
    // actual text, not just a length: before deleting, we confirm it's still sitting right
    // before the cursor, since focus may have moved to a different field entirely while this
    // action window stayed open, and blindly deleting N characters would corrupt whatever's
    // there now. Deletion goes through deleteTextBeforeCursor (direct InputConnection call),
    // not manager.backspace() -- that goes through the legacy InputLogic pipeline, which has
    // its own cached text/cursor state that never learned about anything this action committed.
    private val committedTexts = mutableListOf<String>()

    // Undo history: dictated segments and typed words, most recent last. Typed characters are
    // grouped into a word (closed by whitespace); a backspace shortens the latest entry, so the
    // regular keyboard backspace and Undo stay consistent. No attempt is made to guard against
    // typing and dictation interleaving.
    private var openTypedEntry = false

    private fun pushVoiceEntry(text: String) {
        committedTexts.add(text)
        openTypedEntry = false
    }

    private fun onTypedEvent(codePoint: Int, isDelete: Boolean) {
        if (isDelete) {
            val last = committedTexts.lastOrNull() ?: return
            if (last.length <= 1) {
                committedTexts.removeAt(committedTexts.lastIndex)
                openTypedEntry = false
            } else {
                committedTexts[committedTexts.lastIndex] = last.dropLast(1)
            }
            return
        }
        val ch = String(Character.toChars(codePoint))
        if (openTypedEntry && committedTexts.isNotEmpty()) {
            committedTexts[committedTexts.lastIndex] = committedTexts.last() + ch
        } else {
            committedTexts.add(ch)
        }
        openTypedEntry = !Character.isWhitespace(codePoint)
    }

    init {
        if (inlineMode) TypedTextTap.listener = { cp, del -> onTypedEvent(cp, del) }
    }

    private fun undoLast() {
        val text = committedTexts.lastOrNull() ?: return
        manager.getLifecycleScope().launch(Dispatchers.Main) {
            committedTexts.removeAt(committedTexts.lastIndex)
            openTypedEntry = false
            inputTransaction.deleteTextBeforeCursor(text.length)
        }
    }

    private fun pressEnter() {
        manager.getLifecycleScope().launch(Dispatchers.Main) {
            inputTransaction.commit("\n")
            inputTransaction = newTransaction()
            pushVoiceEntry("\n")
        }
    }

    @Composable
    private fun ModelDownloader(modelException: ModelDoesNotExistException) {
        NoModelInstalled(locales.firstOrNull() ?: Locale.ROOT)
    }

    override val onlyShowAboveKeyboard: Boolean get() = inlineMode
    override val fixedWindowHeight: Dp? get() = if (inlineMode) 0.dp else null
    override val showCloseButton: Boolean get() = !inlineMode
    override val overridesSuggestionBar: Boolean get() = inlineMode

    @Composable
    override fun SuggestionBarOverride() {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .height(ActionBarHeight)
                .padding(horizontal = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = {
                recognizerView.value?.finish() ?: manager.closeActionWindow()
            }) {
                Icon(
                    painter = painterResource(R.drawable.mic_fill),
                    contentDescription = stringResource(R.string.action_voice_input_title),
                    tint = Color(0xFF3B82F6)
                )
            }
            Text(
                text = "Listening…",
                color = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.padding(start = 4.dp)
            )
            Spacer(modifier = Modifier.weight(1.0f))
            IconButton(onClick = { undoLast() }) {
                Icon(
                    painter = painterResource(R.drawable.undo),
                    contentDescription = stringResource(R.string.action_voice_input_undo),
                    tint = MaterialTheme.colorScheme.onSurface
                )
            }
        }
    }

    @Composable
    override fun KeyboardOverlay() {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(0.55f),
            contentAlignment = Alignment.Center
        ) {
            recognizerView.value?.Content()
        }
    }

    @Composable
    override fun windowName(): String {
        return stringResource(R.string.action_voice_input_title)
    }

    @Composable
    override fun WindowContents(keyboardShown: Boolean) {
        if (inlineMode) return
        Box(modifier = Modifier
            .fillMaxSize()
            .clickable(
                enabled = true,
                onClickLabel = null,
                onClick = { recognizerView.value?.finish() },
                role = null,
                indication = null,
                interactionSource = remember { MutableInteractionSource() })
            .semantics(mergeDescendants = true) {
                traversalIndex = -1.0f
            }) {
            Box(modifier = Modifier.align(Alignment.Center)) {
                when {
                    modelException.value != null -> ModelDownloader(modelException.value!!)
                    recognizerView.value != null -> recognizerView.value!!.Content()
                }
            }

            if (useActionButtons && recognizerView.value != null) {
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .padding(end = 16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    IconButton(onClick = { undoLast() }) {
                        Icon(
                            painter = painterResource(R.drawable.undo),
                            contentDescription = stringResource(R.string.action_voice_input_undo),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(onClick = { pressEnter() }) {
                        Icon(
                            painter = painterResource(R.drawable.arrow_down),
                            contentDescription = stringResource(R.string.action_voice_input_enter),
                            tint = MaterialTheme.colorScheme.onSurface
                        )
                    }
                }
            }
        }
    }

    override fun close(): CloseResult {
        TypedTextTap.listener = null
        inputTransaction.cancel()
        runBlocking { initJob.cancelAndJoin() }
        recognizerView.value?.cancel()
        state.modelManager.cancelAll()
        return CloseResult.Default
    }

    private var wasFinished = false
    private var cancelPlayed = false
    override fun cancelled() {
        if (!wasFinished) {
            if (shouldPlaySounds && !cancelPlayed) {
                state.soundPlayer.playCancelSound()
                cancelPlayed = true
            }
            inputTransaction.cancel()
        }
    }

    override fun recordingStarted(device: MicrophoneDeviceState) {
        if (shouldPlaySounds) {
            state.soundPlayer.playStartSound()
        }

        // Only set the setting if bluetooth is available, else it would reset the setting
        // every time it's used without a bluetooth device connected.
        if(device.bluetoothAvailable) {
            manager.getLifecycleScope().launch {
                context.setSetting(PREFER_BLUETOOTH, device.bluetoothActive)
            }
        }
    }

    override fun finished(result: String) {
        wasFinished = true

        manager.getLifecycleScope().launch(Dispatchers.Main) {
            val sanitized = ModelOutputSanitizer.sanitize(result, inputTransaction.textContext)
            inputTransaction.commit(sanitized)
            manager.announce(result)
            manager.closeActionWindow()
        }
    }

    override fun segmentResult(result: String) {
        manager.getLifecycleScope().launch(Dispatchers.Main) {
            val sanitized = ModelOutputSanitizer.sanitize(result, inputTransaction.textContext)
            if (sanitized.isNotBlank()) {
                // Committed for good, no later revision: start a fresh transaction so the
                // next segment's partial/commit calls don't touch what's already locked in.
                val committedText = sanitized.trimEnd() + " "
                inputTransaction.commit(committedText)
                inputTransaction = newTransaction()
                pushVoiceEntry(committedText)
            }
        }
    }

    override fun clickGesture(clickCount: Int) {
        when {
            clickCount == 2 -> pressEnter()
        }
    }

    override fun partialResult(result: String) {
        manager.getLifecycleScope().launch(Dispatchers.Main) {
            val sanitized = ModelOutputSanitizer.sanitize(result, inputTransaction.textContext)
            inputTransaction.updatePartial(sanitized)
        }
    }

    override fun requestPermission(onGranted: () -> Unit, onRejected: () -> Unit): Boolean {
        return false
    }

    override fun openSettings() {
        SettingsActivity.openToNavDest(context, "languages")
    }
}

private class VoiceInputNoModelWindow(val locale: Locale) : ActionWindow() {
    @Composable
    override fun windowName(): String {
        return stringResource(R.string.action_voice_input_title)
    }

    @Composable
    override fun WindowContents(keyboardShown: Boolean) {
        NoModelInstalled(locale)
    }
}

val VoiceInputAction = Action(icon = R.drawable.mic_fill,
    name = R.string.action_voice_input_title,
    simplePressImpl = null,
    keepScreenAwake = true,
    persistentState = { VoiceInputPersistentState(it) },
    windowImpl = { manager, persistentState ->
        val locales = manager.getActiveLocales()

        val model = ResourceHelper.tryFindingVoiceInputModelForLocale(manager.getContext(), locales.firstOrNull() ?: Locale.ROOT)

        if(model == null) {
            VoiceInputNoModelWindow(locales.firstOrNull() ?: Locale.ROOT)
        } else {
            VoiceInputActionWindow(
                manager = manager, state = persistentState as VoiceInputPersistentState,
                locales = locales, model = model
            )
        }
    }
)