package org.futo.inputmethod.engine.general

import android.view.inputmethod.EditorInfo
import org.futo.inputmethod.engine.IMEHelper
import org.futo.inputmethod.engine.IMEInterface
import org.futo.inputmethod.event.Event
import org.futo.inputmethod.latin.InputConnectionInternalComposingWrapper
import org.futo.inputmethod.latin.SupportsNonComposing
import org.futo.inputmethod.latin.VoiceInputAlternativeIC
import org.futo.inputmethod.latin.VoiceInputAlternativeICComposing
import org.futo.inputmethod.latin.common.Constants
import org.futo.inputmethod.latin.common.InputPointers
import org.futo.inputmethod.latin.uix.ActionInputTransaction
import org.futo.inputmethod.latin.uix.getSetting
import org.futo.inputmethod.latin.uix.utils.TextContext
import org.futo.inputmethod.latin.utils.InputTypeUtils
import org.futo.inputmethod.v2keyboard.KeyboardLayoutSetV2

class ActionInputTransactionIME(val helper: IMEHelper) : IMEInterface, ActionInputTransaction {
    val useComposingMode = run {
        val inputType = helper.getCurrentEditorInfo()?.inputType ?: 0
        val inputClass = inputType and EditorInfo.TYPE_MASK_CLASS
        inputClass == EditorInfo.TYPE_CLASS_TEXT
    }

    val ic = if(helper.context.getSetting(VoiceInputAlternativeIC) && SupportsNonComposing && useComposingMode) {
        InputConnectionInternalComposingWrapper(
            helper.context.getSetting(VoiceInputAlternativeICComposing),
            true,
            helper.getCurrentInputConnection())
    } else {
        helper.getCurrentInputConnection()
    }

    override fun onCreate() {}
    override fun onDestroy() {}
    override fun onDeviceUnlocked() {}
    override fun onStartInput() {}
    override fun onOrientationChanged() {}
    override fun onFinishInput() {}
    override fun onUpdateSelection(
        oldSelStart: Int,
        oldSelEnd: Int,
        newSelStart: Int,
        newSelEnd: Int,
        composingSpanStart: Int,
        composingSpanEnd: Int
    ) {
        if(ic is InputConnectionInternalComposingWrapper) {
            if(!ic.mightBeBelated(oldSelStart, oldSelEnd, newSelStart, newSelEnd)) {
                ic.cursorUpdated(oldSelStart, oldSelEnd, newSelStart, newSelEnd)
            }
        }
    }

    override fun isGestureHandlingAvailable(): Boolean = false
    override fun onEvent(event: Event) {}
    override fun onStartBatchInput() {}
    override fun onUpdateBatchInput(batchPointers: InputPointers?) {}
    override fun onEndBatchInput(batchPointers: InputPointers?) {}
    override fun onCancelBatchInput() {}
    override fun onCancelInput() {}
    override fun onFinishSlidingInput() {}
    override fun onCustomRequest(requestCode: Int): Boolean = false
    override fun onMovePointer(steps: Int, stepOverWords: Boolean, select: Boolean?) {}
    override fun onMoveDeletePointer(steps: Int) {}
    override fun onUpWithDeletePointerActive() {}
    override fun onUpWithPointerActive() {}
    override fun onSwipeLanguage(direction: Int) {}
    override fun onMovingCursorLockEvent(canMoveCursor: Boolean) {}
    override fun clearUserHistoryDictionaries() {}
    override fun requestSuggestionRefresh() {}
    override fun onLayoutUpdated(layout: KeyboardLayoutSetV2) { }

    override val textContext: TextContext = TextContext(
        beforeCursor = ic?.getTextBeforeCursor(Constants.VOICE_INPUT_CONTEXT_SIZE, 0),
        afterCursor = ic?.getTextAfterCursor(Constants.VOICE_INPUT_CONTEXT_SIZE, 0)
    )

    private var isFinished = false
    private var partialText = ""
    override fun updatePartial(text: String) {
        if (isFinished || !useComposingMode) return
        helper.requestCursorUpdate()
        partialText = text
        ic?.setComposingText(
            partialText,
            1
        )

        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    override fun commit(text: String) {
        if (isFinished) return
        helper.requestCursorUpdate()
        isFinished = true
        ic?.commitText(
            text,
            1
        )
        helper.endInputTransaction(this)
        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    override fun deleteTextBeforeCursor(length: Int) {
        if (length <= 0) return
        helper.requestCursorUpdate()
        ic?.deleteSurroundingText(length, 0)
        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    override fun liveTextBeforeCursor(length: Int): String? =
        ic?.getTextBeforeCursor(length, 0)?.toString()

    override fun performEditorAction() {
        val editorInfo = helper.getCurrentEditorInfo() ?: return

        // Mirrors InputLogic's handling of the real Enter key (CODE_ENTER) for an editor action.
        val imeOptionsActionId = InputTypeUtils.getImeOptionsActionIdFromEditorInfo(editorInfo)
        val isCustomAction = InputTypeUtils.IME_ACTION_CUSTOM_LABEL == imeOptionsActionId
        val isEditorAction = EditorInfo.IME_ACTION_NONE != imeOptionsActionId

        if (isCustomAction) {
            ic?.performEditorAction(editorInfo.actionId)
        } else if (isEditorAction) {
            ic?.performEditorAction(imeOptionsActionId)
        }
        else {
            // Multi-line fields declare no action; fall back to Ctrl+Enter, which the Claude app
            // treats as Send. Sent as a real key sequence (Ctrl down, Enter down/up, Ctrl up),
            // the same as a hardware keyboard, since apps may watch for the Ctrl key itself.
            val now = android.os.SystemClock.uptimeMillis()
            val ctrl = android.view.KeyEvent.META_CTRL_ON or android.view.KeyEvent.META_CTRL_LEFT_ON
            fun key(action: Int, code: Int, meta: Int) =
                ic?.sendKeyEvent(android.view.KeyEvent(now, now, action, code, 0, meta))
            key(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_CTRL_LEFT, ctrl)
            key(android.view.KeyEvent.ACTION_DOWN, android.view.KeyEvent.KEYCODE_ENTER, ctrl)
            key(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_ENTER, ctrl)
            key(android.view.KeyEvent.ACTION_UP, android.view.KeyEvent.KEYCODE_CTRL_LEFT, 0)
        }
    }

    override fun cancel() {
        helper.requestCursorUpdate()
        commit(partialText)
        (ic as? InputConnectionInternalComposingWrapper)?.send()
    }

    fun ensureFinished() {
        isFinished = true
    }
}