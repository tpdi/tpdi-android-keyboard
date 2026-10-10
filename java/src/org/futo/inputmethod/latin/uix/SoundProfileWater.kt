package org.futo.inputmethod.latin.uix

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlin.math.PI
import kotlin.math.pow
import kotlin.math.sin
import org.futo.inputmethod.keyboard.Key

/**
 * Draws the selected sound profile on the microphone key as bluish water: empty for no masking
 * (Quiet room), a third full for TV on, two thirds full for Outside / noisy. Does nothing while sound
 * profiles are off.
 */
object SoundProfileWater {
    @Volatile
    private var level = 0f

    @Volatile
    private var active = false

    private val water = Paint(Paint.ANTI_ALIAS_FLAG).apply { color = 0xB08DBBFF.toInt() }
    private val surface = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = 0xFFDCEBFF.toInt()
        style = Paint.Style.STROKE
        strokeWidth = 3f
    }
    private val rect = RectF()
    private val surfacePath = Path()
    private val fillPath = Path()
    private val clipPath = Path()
    private const val RIPPLES = 2.0

    @JvmStatic
    fun draw(key: Key, canvas: Canvas, width: Int, height: Int) {
        val l = level
        if (l <= 0f || !SoundProfiles.isMicKey(key.code)) return
        val inset = minOf(width, height) * 0.06f
        val left = inset
        val right = width - inset
        val bottom = height - inset
        val top = inset + (height - 2 * inset) * (1f - l)
        val radius = minOf(width, height) * 0.16f
        // The fuller the key, the bigger the ripples.
        val amplitude = (height - 2 * inset) * 0.08f * (l / 0.65f).pow(1.164f)
        val steps = 90
        surfacePath.reset()
        for (i in 0..steps) {
            val x = left + (right - left) * i / steps
            val y = top + amplitude * sin(i * 2.0 * PI * RIPPLES / steps).toFloat()
            if (i == 0) surfacePath.moveTo(x, y) else surfacePath.lineTo(x, y)
        }
        fillPath.set(surfacePath)
        fillPath.lineTo(right, bottom)
        fillPath.lineTo(left, bottom)
        fillPath.close()
        rect.set(left, top - amplitude, right, bottom)
        clipPath.reset()
        clipPath.addRoundRect(rect, radius, radius, Path.Direction.CW)
        canvas.save()
        canvas.clipPath(clipPath)
        canvas.drawPath(fillPath, water)
        canvas.drawPath(surfacePath, surface)
        canvas.restore()
    }

    /** The microphone is drawn most of its key while profiles are on, so it shows over the water. */
    @JvmStatic
    fun iconSize(code: Int, normalSize: Int, keySize: Int): Int =
        if (active && SoundProfiles.isMicKey(code)) maxOf(normalSize, (keySize * 0.75f).toInt()) else normalSize

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
            active = context.getSetting(SOUND_PROFILES)
            redraw()
        }
    }
}
