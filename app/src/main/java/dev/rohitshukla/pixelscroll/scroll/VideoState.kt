package dev.rohitshukla.pixelscroll.scroll

/**
 * What the engine knows about the Short on screen: when it started, its length, whether our
 * swipe is waiting to land, and whether to read the progress bar yet. Pure Kotlin: the caller
 * passes in the time.
 */
class VideoState {

    /** The Short's length from the seek bar, or null until it is read. */
    var lengthSec: Int? = null
        private set

    /** Changes with every new video, so work timed for one video never runs on the next. */
    var token = 0
        private set

    private var startedAt = 0L
    private var swipedAt = 0L
    private var readFromStart = false

    /**
     * A new video is on screen. [readFromStart] reads progress for the whole video: use it when
     * Shorts opens, since the first Short may already be part-way through.
     */
    fun start(nowMs: Long, readFromStart: Boolean = false) {
        token++
        lengthSec = null
        startedAt = nowMs
        swipedAt = 0L
        this.readFromStart = readFromStart
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

    /**
     * Whether to read the progress bar now. With a known length, only from [READ_LEAD_MS]
     * before the expected end: enough readings to catch the end, without copying the screen
     * every second of the video.
     */
    fun shouldReadProgress(nowMs: Long): Boolean {
        val length = lengthSec ?: return true
        if (readFromStart) return true
        return nowMs - startedAt >= length * 1000L - READ_LEAD_MS
    }

    /** With a known length, the fallback is the length plus one second; else the user's delay. */
    fun config(fallbackDelaySec: Int): ScrollConfig =
        ScrollConfig(fallbackDelayMs = lengthSec?.let { (it + 1) * 1000L } ?: (fallbackDelaySec * 1000L))

    companion object {
        /** After our swipe, wait this long for a scroll event before assuming a new video. */
        const val NO_SCROLL_EVENT_MS = 3_000L

        /** Start reading the progress bar this long before a Short's expected end. */
        const val READ_LEAD_MS = 5_000L
    }
}
