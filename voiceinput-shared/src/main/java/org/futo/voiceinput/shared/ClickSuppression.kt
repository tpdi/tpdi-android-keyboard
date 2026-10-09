package org.futo.voiceinput.shared

/**
 * Key taps on the on-screen keyboard make a sharp sound the microphone hears as a click. The
 * keyboard records when keys are pressed so click detection can ignore those moments.
 */
object ClickSuppression {
    @Volatile
    var lastKeyPressMs: Long = 0L
}
