package org.futo.inputmethod.latin.uix

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.RectF
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import org.futo.inputmethod.keyboard.Key

/**
 * Draws the selected sound profile on the microphone key as bluish water: empty for no masking
 * (Quiet room), a third full for TV on, two thirds full for Outside / noisy. Does nothing while sound
 * profiles are off.
 */
object SoundProfileWater {
    @Volatile
    private var level = 0f

    private val water = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0x66B8D4FF.toInt() }
    private val surface = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xAAD8E8FF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val rect = RectF()

    @JvmStatic
    fun draw(key: Key, canvas: Canvas, width: Int, height: Int) {
        val l = level
        if (l <= 0f || !SoundProfiles.isMicKey(key.code)) return
        val inset = minOf(width, height) * 0.06f
        val top = inset + (height - 2 * inset) * (1f - l)
        rect.set(inset, top, width - inset, height - inset)
        val radius = minOf(width, height) * 0.16f
        canvas.drawRoundRect(rect, radius, radius, water)
        canvas.drawLine(inset + radius * 0.5f, top, width - inset - radius * 0.5f, top, surface)
    }

    /** Recomputes the water level and calls [redraw] when it changed. */
    suspend fun watch(context: Context, redraw: () -> Unit) {
        combine(
            context.getSettingFlow(SOUND_PROFILES),
            context.getSettingFlow(SOUND_PROFILE_ACTIVE),
            context.getSettingFlow(SOUND_PROFILES_CUSTOM)
        ) { on, active, _ ->
            if (!on) 0f else SoundProfiles.waterLevel(SoundProfiles.all(context).firstOrNull { it.name == active })
        }.distinctUntilChanged().collect {
            level = it
            redraw()
        }
    }
}
