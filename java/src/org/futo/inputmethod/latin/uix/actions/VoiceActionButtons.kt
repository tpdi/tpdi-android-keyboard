package org.futo.inputmethod.latin.uix.actions

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.R

/** Undo and Enter buttons shown beside the voice input bubble. */
@Composable
fun VoiceActionButtons(onUndo: () -> Unit, onEnter: () -> Unit, modifier: Modifier = Modifier) {
    Column(modifier = modifier, verticalArrangement = Arrangement.spacedBy(12.dp)) {
        IconButton(onClick = onUndo) {
            Icon(
                painter = painterResource(R.drawable.undo),
                contentDescription = stringResource(R.string.action_voice_input_undo),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        IconButton(onClick = onEnter) {
            Icon(
                painter = painterResource(R.drawable.sym_keyboard_return_lxx_dark),
                contentDescription = stringResource(R.string.action_voice_input_enter),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
