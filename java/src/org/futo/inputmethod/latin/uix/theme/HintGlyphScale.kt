package org.futo.inputmethod.latin.uix.theme

import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import java.util.concurrent.ConcurrentHashMap

/**
 * [scale] multiplies the hint size. [centerY] is where the glyph's vertical center sits below the top
 * of the font box (which is where hints are anchored), as a fraction of the size.
 */
data class GlyphScale(val scale: Float, val centerY: Float)

/**
 * A per-glyph size multiplier for key hints, so a quote mark and a brace come out about the same
 * size. Glyphs are only ever scaled up (the other hint controls scale everything down): a glyph
 * whose bounding box is already large enough, 60% of the font's cap height in its larger side,
 * is left at 1; a smaller one is scaled up to that size, at most [MAX_SCALE].
 *
 * The glyph's vertical center is kept where it is unscaled (see [KeyDrawingConfiguration.hintOffsetY]),
 * so a dash stays on its line and a comma does not sink.
 *
 * The multiplier depends on the font, so it is measured once per (typeface, character) and kept in
 * a lookup table. Drawing only does a table lookup.
 */
object HintGlyphScale {
    private const val MEASURE_SIZE = 100f
    /** A glyph whose larger side is at least this fraction of the cap height is left alone. */
    private const val TARGET_FRACTION = 0.6f
    private const val MAX_SCALE = 1.8f

    private val table = ConcurrentHashMap<Long, GlyphScale>()
    private val unchanged = GlyphScale(1.0f, 0.0f)
    private val paint = Paint()
    private val bounds = Rect()

    /** The scale for [hint] in [typeface]; unchanged for anything that is not a single character. */
    @JvmStatic
    fun scaleFor(hint: String?, typeface: Typeface): GlyphScale {
        if (hint == null || hint.codePointCount(0, hint.length) != 1) return unchanged
        val codePoint = hint.codePointAt(0)
        val key = (System.identityHashCode(typeface).toLong() shl 32) or codePoint.toLong()
        return table[key] ?: measure(hint, typeface).also { table[key] = it }
    }

    @Synchronized
    private fun measure(hint: String, typeface: Typeface): GlyphScale {
        paint.typeface = typeface
        paint.textSize = MEASURE_SIZE

        paint.getTextBounds("H", 0, 1, bounds)
        val capHeight = bounds.height().toFloat()

        paint.getTextBounds(hint, 0, hint.length, bounds)
        val glyph = maxOf(bounds.width(), bounds.height()).toFloat()

        if (capHeight <= 0f || glyph <= 0f) return unchanged
        val centerY = (-paint.ascent() + (bounds.top + bounds.bottom) / 2f) / MEASURE_SIZE
        return GlyphScale((capHeight * TARGET_FRACTION / glyph).coerceIn(1.0f, MAX_SCALE), centerY)
    }
}
