package dev.rohitshukla.pixelscroll.data

/** The user's settings: both live on the Home screen. */
data class Settings(
    /** Auto-scroll on or off: the power card switch. */
    val enabled: Boolean = true,
    /** Time per Short when its length can't be read. */
    val fallbackDelaySec: Int = DEFAULT_DELAY_SEC,
) {
    companion object {
        const val MIN_DELAY_SEC = 3
        const val MAX_DELAY_SEC = 120
        const val DEFAULT_DELAY_SEC = 15

        fun clampDelay(seconds: Int) = seconds.coerceIn(MIN_DELAY_SEC, MAX_DELAY_SEC)
    }
}
