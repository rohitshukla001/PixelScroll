package dev.rohitshukla.pixelscroll.service

/** A straight vertical swipe: one x position, from startY to endY, in screen pixels. */
data class SwipeLine(val x: Float, val startY: Float, val endY: Float)

object SwipeGeometry {
    private const val START_FRACTION = 0.8f
    private const val END_FRACTION = 0.2f

    /**
     * An upward swipe through the middle of an area, from 80% to 20% of its height.
     * Returns null for an empty area.
     */
    fun upward(left: Int, top: Int, width: Int, height: Int): SwipeLine? {
        if (width <= 0 || height <= 0) return null
        return SwipeLine(
            x = left + width / 2f,
            startY = top + height * START_FRACTION,
            endY = top + height * END_FRACTION,
        )
    }
}
