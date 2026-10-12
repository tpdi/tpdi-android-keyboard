package org.futo.inputmethod.latin.uix.actions

import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import org.futo.inputmethod.latin.R

/** Ends voice input at once: stops recording and drops any transcription still running. */
@Composable
fun VoiceAbortButton(onClick: () -> Unit, modifier: Modifier = Modifier) {
    IconButton(onClick = onClick, modifier = modifier) {
        Icon(
            painter = painterResource(R.drawable.close),
            contentDescription = stringResource(R.string.voice_input_abort),
            tint = MaterialTheme.colorScheme.onSurface
        )
    }
}
