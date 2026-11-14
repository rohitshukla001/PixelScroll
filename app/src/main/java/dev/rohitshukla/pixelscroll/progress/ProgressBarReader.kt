package dev.rohitshukla.pixelscroll.progress

/**
 * Reads how far a thin progress bar is filled, from screenshot pixels. Pure Kotlin.
 *
 * YouTube Shorts draw the played part as a light-grey line, 6 px high at 1080 x 2400
 * (docs/progress-detection-test.md). For each candidate row the reader takes the longest run
 * of light pixels that starts at the bar's left edge. A run only counts when the rows a few
 * pixels above and below are mostly not light, so bright video content is not mistaken for
 * the bar.
 */
object ProgressBarReader {

    /** Gap between a bar row and the rows checked above and below it. */
    private const val EDGE_GAP = 6

    /**
     * [pixels] is an ARGB region [width] x [height], row by row, cut from the screenshot so that
     * x = 0 is the bar's left edge and x = width - 1 its right edge.
     * Returns the fill from 0 to 1, or null when no bar-like line is found.
     */
    fun fill(pixels: IntArray, width: Int, height: Int): Float? {
        if (width <= 0 || height <= 0 || pixels.size < width * height) return null
        val startSlack = maxOf(4, width / 30)
        var best = -1
        for (y in 0 until height) {
            val start = (0 until minOf(startSlack, width)).firstOrNull { isLight(pixels[y * width + it]) } ?: continue
            var end = start
            while (end + 1 < width && isLight(pixels[y * width + end + 1])) end++
            if (end - start < 2) continue
            if (isThinLine(pixels, width, height, y, start, end) && end + 1 > best) best = end + 1
        }
        return if (best < 0) null else (best.toFloat() / width).coerceIn(0f, 1f)
    }

    private fun isThinLine(pixels: IntArray, width: Int, height: Int, y: Int, start: Int, end: Int): Boolean {
        fun lightShare(row: Int): Float {
            if (row !in 0 until height) return 0f
            val n = (start..end).count { isLight(pixels[row * width + it]) }
            return n.toFloat() / (end - start + 1)
        }
        return lightShare(y - EDGE_GAP) < 0.5f && lightShare(y + EDGE_GAP) < 0.5f
    }

    /** Light and grey: all channels above 150 and close to each other. */
    internal fun isLight(argb: Int): Boolean {
        val r = (argb shr 16) and 0xFF
        val g = (argb shr 8) and 0xFF
        val b = argb and 0xFF
        val lo = minOf(r, g, b)
        val hi = maxOf(r, g, b)
        return lo > 150 && hi - lo < 40
    }
}
