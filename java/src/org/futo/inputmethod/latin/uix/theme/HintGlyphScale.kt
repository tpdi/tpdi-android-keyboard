package org.futo.inputmethod.latin.uix.theme

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.util.concurrent.ConcurrentHashMap

/**
 * A per-glyph size multiplier for key hints, so a quote mark and a brace come out about the same
 * size. The glyph's tight bounding box is scaled until its larger side equals the cap height of
 * the font, within [MIN_SCALE]..[MAX_SCALE].
 *
 * The multiplier depends on the font, so it is measured once per (typeface, character) and kept in
 * a lookup table. Drawing only does a table lookup.
 */
object HintGlyphScale {
    private const val MEASURE_SIZE = 100f
    private const val MIN_SCALE = 0.6f
    private const val MAX_SCALE = 1.8f

    private val table = ConcurrentHashMap<Long, Float>()
    private val paint = Paint()
    private val bounds = Rect()

    /** The multiplier for [hint] in [typeface]; 1 for anything that is not a single character. */
    @JvmStatic
    fun multiplierFor(hint: String?, typeface: Typeface): Float {
        if (hint == null || hint.codePointCount(0, hint.length) != 1) return 1.0f
        val codePoint = hint.codePointAt(0)
        val key = (System.identityHashCode(typeface).toLong() shl 32) or codePoint.toLong()
        return table[key] ?: measure(hint, typeface).also { table[key] = it }
    }

    @Synchronized
    private fun measure(hint: String, typeface: Typeface): Float {
        paint.typeface = typeface
        paint.textSize = MEASURE_SIZE

        paint.getTextBounds("H", 0, 1, bounds)
        val capHeight = bounds.height().toFloat()

        paint.getTextBounds(hint, 0, hint.length, bounds)
        val glyph = maxOf(bounds.width(), bounds.height()).toFloat()

        if (capHeight <= 0f || glyph <= 0f) return 1.0f
        return (capHeight / glyph).coerceIn(MIN_SCALE, MAX_SCALE)
    }
}
