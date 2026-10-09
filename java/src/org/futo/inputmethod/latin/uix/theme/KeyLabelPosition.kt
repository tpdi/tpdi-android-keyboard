package org.futo.inputmethod.latin.uix.theme

/**
 * How far to move a key label to the left of the key centre. [anchor] 0 keeps it centred; 1 puts
 * its left edge a small margin in from the key's left edge. Narrow labels move the furthest.
 */
object KeyLabelPosition {
    private const val EDGE_MARGIN = 0.12f // of the key width

    @JvmStatic
    fun shiftX(anchor: Float, keyWidth: Int, labelWidth: Float): Float {
        if (anchor <= 0f) return 0f
        val slack = keyWidth / 2f - labelWidth / 2f - keyWidth * EDGE_MARGIN
        return if (slack > 0f) anchor * slack else 0f
    }
}
