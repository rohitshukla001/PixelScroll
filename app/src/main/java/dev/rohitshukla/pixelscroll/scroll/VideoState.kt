package dev.rohitshukla.pixelscroll.scroll

/**
 * What the engine knows about the Short on screen: its length, whether our
 * swipe is waiting to land, and when to read the progress bar next. Pure Kotlin: the caller
 * passes in the time.
 *
 * Reading times follow the video's real position, not the clock, so jumping forward or back in
 * a Short still ends in a swipe at its real end.
 */
class VideoState {

    /** The Short's length from the seek bar, or null until it is read. */
    var lengthSec: Int? = null
        private set

    /** Changes with every new video, so work timed for one video never runs on the next. */
    var token = 0
        private set

    private var swipedAt = 0L
    private var nextReadAt = 0L

    /** A new video is on screen. The first progress reading is due at once. */
    fun start(nowMs: Long) {
        token++
        lengthSec = null
        swipedAt = 0L
        nextReadAt = nowMs
    }

    /** Shorts closed or auto-scroll stopped: anything timed for this video must not run. */
    fun clear() {
        token++
        lengthSec = null
        swipedAt = 0L
    }

    /**
     * The length the seek bar shows now. Returns true when it differs from the known length,
     * which means a new Short (the user swiped without a scroll event reaching us).
     */
    fun onLength(length: Int?, nowMs: Long): Boolean {
        if (length == null) return false
        val known = lengthSec
        val changed = known != null && known != length
        if (changed) start(nowMs)
        lengthSec = length
        return changed
    }

    fun onSwiped(nowMs: Long) {
        swipedAt = nowMs
    }

    /**
     * True when our swipe was sent [NO_SCROLL_EVENT_MS] ago and no scroll event followed:
     * assume it landed and a new video is on screen.
     */
    fun swipeLandedSilently(nowMs: Long): Boolean = swipedAt != 0L && nowMs - swipedAt > NO_SCROLL_EVENT_MS

    /** Whether to read the progress bar now. Always, until the length is known. */
    fun shouldReadProgress(nowMs: Long): Boolean = lengthSec == null || nowMs + READ_SLACK_MS >= nextReadAt

    /**
     * A progress reading ([fill], 0 to 1) arrived. Sets the next reading from where the video
     * really is: every second from [READ_LEAD_MS] before its end, else every [SAMPLE_MS]. A jump
     * forward or back shows up in the next sample and moves the end with it.
     */
    fun onProgressRead(fill: Float, nowMs: Long) {
        val lengthMs = lengthSec?.times(1000L) ?: return
        val remainingMs = ((1f - fill.coerceIn(0f, 1f)) * lengthMs).toLong()
        nextReadAt = if (remainingMs <= READ_LEAD_MS) nowMs else nowMs + minOf(SAMPLE_MS, remainingMs - READ_LEAD_MS)
    }

    /** With a known length, the fallback is the length plus one second; else the user's delay. */
    fun config(fallbackDelaySec: Int): ScrollConfig =
        ScrollConfig(fallbackDelayMs = lengthSec?.let { (it + 1) * 1000L } ?: (fallbackDelaySec * 1000L))

    companion object {
        /** After our swipe, wait this long for a scroll event before assuming a new video. */
        const val NO_SCROLL_EVENT_MS = 3_000L

        /** Read the progress bar every second from this long before a Short's end. */
        const val READ_LEAD_MS = 5_000L

        /**
         * Before that, read it this often. Under ScrollDecider's 4.5 s progress timeout, so the
         * fallback delay never fires while the bar is being read (a rewound Short isn't swiped).
         */
        const val SAMPLE_MS = 3_000L

        /**
         * A reading lands a few ms after its tick, so the tick that is due arrives slightly
         * early; without this slack it would wait for the tick after, a 4 s gap.
         */
        const val READ_SLACK_MS = 500L
    }
}
