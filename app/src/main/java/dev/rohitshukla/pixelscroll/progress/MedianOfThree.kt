package dev.rohitshukla.pixelscroll.progress

/**
 * Smooths progress readings with the median of the last three, so one wrong reading (for
 * example bright video content next to the bar) can't trigger a swipe. Adds one reading of delay.
 */
class MedianOfThree {
    private val last = ArrayDeque<Float>(3)

    fun reset() = last.clear()

    /** Returns null until three readings have arrived. */
    fun add(value: Float): Float? {
        if (last.size == 3) last.removeFirst()
        last.addLast(value)
        return if (last.size < 3) null else last.sorted()[1]
    }
}
