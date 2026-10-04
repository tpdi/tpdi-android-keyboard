package org.futo.inputmethod.latin.uix.actions

import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.Action

// Closes the keyboard, like the down-chevron key on Samsung's keyboard. Not shown anywhere unless
// a layout uses it (`!code/action_dismiss_keyboard`), so by default nothing changes.
val DismissKeyboardAction = Action(
    icon = R.drawable.keyboard_dismiss,
    name = R.string.action_dismiss_keyboard_title,
    simplePressImpl = { manager, _ ->
        manager.getLatinIMEForDebug().requestHideSelf(0)
    },
    windowImpl = null,
)
