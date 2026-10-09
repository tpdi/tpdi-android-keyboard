package org.futo.inputmethod.latin.uix.actions

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.layout
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInRoot
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import org.futo.inputmethod.latin.R
import org.futo.inputmethod.latin.uix.ActionBarHeight
import org.futo.inputmethod.latin.uix.ActionBarMicPosition
import org.futo.inputmethod.latin.uix.LocalKeyboardScheme

/**
 * The suggestion bar while dictating over the keyboard: "Listening...", an Undo button, and a
 * blue microphone placed exactly where the action bar's own microphone icon is. Tapping the
 * microphone ends the session.
 */
@Composable
fun VoiceListeningBar(circle: @Composable () -> Unit, onUndo: () -> Unit, onStop: () -> Unit) {
    val density = LocalDensity.current
    var barLeft by remember { mutableStateOf(0f) }
    var barWidth by remember { mutableStateOf(0) }
    val micWidthPx = with(density) { 42.dp.toPx() }
    val undoWidthPx = with(density) { 48.dp.toPx() }
    val gapPx = with(density) { 20.dp.toPx() }

    // The blue microphone sits at the middle of the bar; the volume circle radiates from there
    // (see VoiceVolumeCircleOverlay).
    val micCenter = barWidth / 2f
    val micLeft = (micCenter - micWidthPx / 2f)
        .coerceIn(0f, (barWidth - micWidthPx).coerceAtLeast(0f))
    val undoLeft = (micLeft - gapPx - undoWidthPx).coerceAtLeast(0f)

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(ActionBarHeight)
            .onGloballyPositioned {
                barLeft = it.positionInRoot().x
                barWidth = it.size.width
            }
    ) {
        // The volume circle also covers the bar (the keyboard overlay draws the rest), centered
        // on the bar, behind the controls.
        Box(
            modifier = Modifier
                .matchParentSize()
                .clipToBounds()
                .alpha(0.55f),
            contentAlignment = Alignment.Center
        ) {
            circle()
        }
        Text(
            text = "Listening…",
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .padding(start = 16.dp)
        )
        IconButton(
            onClick = onUndo,
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(undoLeft.toInt(), 0) }
        ) {
            Icon(
                painter = painterResource(R.drawable.undo),
                contentDescription = stringResource(R.string.action_voice_input_undo),
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
        val pillColor = LocalKeyboardScheme.current.keyboardContainer
        val pillRadiusPx = with(density) { 16.dp.toPx() }
        Box(
            modifier = Modifier
                .align(Alignment.CenterStart)
                .offset { IntOffset(micLeft.toInt(), 0) }
                .width(42.dp)
                .fillMaxHeight()
                .drawBehind { drawCircle(color = pillColor, radius = pillRadiusPx) }
                .clip(CircleShape)
                .clickable { onStop() },
            contentAlignment = Alignment.Center
        ) {
            Icon(
                painter = painterResource(R.drawable.mic_fill),
                contentDescription = stringResource(R.string.action_voice_input_title),
                tint = Color(0xFF3B82F6),
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

/** The volume circle drawn translucently over the keys. */
@Composable
fun VoiceVolumeCircleOverlay(circle: @Composable () -> Unit) {
    val barHeightPx = with(LocalDensity.current) { ActionBarHeight.roundToPx() }
    Box(
        modifier = Modifier
            .fillMaxSize()
            .alpha(0.55f),
        contentAlignment = Alignment.TopCenter
    ) {
        // Centered horizontally, vertically on the middle of the bar above the keys, so the
        // circle radiates from the microphone; its top part is cut off by the screen edge.
        Box(
            modifier = Modifier.layout { measurable, constraints ->
                val p = measurable.measure(constraints.copy(minWidth = 0, minHeight = 0))
                layout(p.width, p.height) {
                    p.place(0, -p.height / 2 - barHeightPx / 2)
                }
            }
        ) {
            circle()
        }
    }
}
