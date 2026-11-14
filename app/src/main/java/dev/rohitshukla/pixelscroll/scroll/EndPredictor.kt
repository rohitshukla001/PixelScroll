package dev.rohitshukla.pixelscroll.scroll

/**
 * Works out when the current video ends, so the swipe lands at the end instead of after the
 * replay has started. Progress is read once a second, so waiting for a reading at the very end
 * usually misses it. Pure Kotlin.
 */
object EndPredictor {

    /** Start the swipe this early, so the next video is on screen when this one ends. */
    const val SWIPE_LEAD_MS = 250L

    /**
     * [raw] is the latest progress reading and [smooth] the filtered one, both 0 to 1.
     * Returns how long to wait before swiping, when the end comes before the next reading
     * ([readingIntervalMs]); otherwise null.
     */
    fun delayToEnd(raw: Float, smooth: Float, lengthMs: Long, readingIntervalMs: Long): Long? {
        if (lengthMs <= 0 || raw.isNaN() || smooth.isNaN()) return null
        // Trust the raw reading only late in the video and when the filter agrees with it,
        // so one bad reading or a loop back to the start can't cause an early swipe.
        if (smooth < 0.5f || raw < smooth - 0.05f) return null
        val remainingMs = ((1f - raw.coerceIn(0f, 1f)) * lengthMs).toLong()
        if (remainingMs > readingIntervalMs + SWIPE_LEAD_MS) return null
        return (remainingMs - SWIPE_LEAD_MS).coerceAtLeast(0)
    }
}
