package dev.rohitshukla.pixelscroll.scroll

/**
 * Decides when to swipe to the next video. Pure Kotlin: the caller passes in the time,
 * so the logic runs the same in unit tests and on a phone.
 *
 * The caller (the scroll engine) must:
 * - call [onNewVideo] whenever a new video starts, including after the user swipes by hand
 *   and after our own swipe lands;
 * - while it reads progress, pass readings to [onProgress] more often than the progress timeout, so a paused or
 *   rewound video keeps the progress fresh and the fallback delay never fires on it (see
 *   [VideoState.SAMPLE_MS]);
 * - call [onTick] on every tick, which drives the fixed-delay fallback.
 *
 * After it returns [Decision.SwipeNow], the decider waits until [onNewVideo] is called again,
 * so one video never causes two swipes.
 */
class ScrollDecider(var config: ScrollConfig = ScrollConfig()) {

    sealed interface Decision {
        data object Wait : Decision
        data class SwipeNow(val reason: Reason) : Decision
    }

    enum class Reason { END_REACHED, LOOPED, FALLBACK_DELAY }

    private var videoStartMs: Long? = null
    private var lastFraction: Float? = null
    private var lastSampleMs: Long? = null
    private var swiped = false

    /** A new video is on screen. Resets everything about the previous one. */
    fun onNewVideo(nowMs: Long) {
        videoStartMs = nowMs
        lastFraction = null
        lastSampleMs = null
        swiped = false
    }

    /** Scrolling stopped (screen off, left the target app, service off). */
    fun stop() {
        videoStartMs = null
        lastFraction = null
        lastSampleMs = null
        swiped = false
    }

    /** A progress reading from 0 (start) to 1 (end). Readings outside that range are clamped. */
    fun onProgress(fraction: Float, nowMs: Long): Decision {
        val start = videoStartMs ?: return Decision.Wait
        if (swiped || fraction.isNaN()) return Decision.Wait

        val current = fraction.coerceIn(0f, 1f)
        val previous = lastFraction
        lastFraction = current
        lastSampleMs = nowMs

        // Readings right after a video change can still describe the previous video.
        if (nowMs - start < config.minWatchMs) return Decision.Wait

        return when {
            isLoop(previous, current) -> swipe(Reason.LOOPED)
            current >= config.endThreshold -> swipe(Reason.END_REACHED)
            else -> Decision.Wait
        }
    }

    /** Called on every tick. Swipes after the fallback delay when no progress is being read. */
    fun onTick(nowMs: Long): Decision {
        val start = videoStartMs ?: return Decision.Wait
        if (swiped) return Decision.Wait

        val lastSample = lastSampleMs
        val progressIsFresh = lastSample != null && nowMs - lastSample < config.progressTimeoutMs
        if (progressIsFresh) return Decision.Wait

        return if (nowMs - start >= config.fallbackDelayMs) swipe(Reason.FALLBACK_DELAY) else Decision.Wait
    }

    /**
     * A loop: the bar was near the end and is now near the start. A rewind (the user jumps back
     * to watch part again) lands in the middle, so it doesn't count.
     */
    private fun isLoop(previous: Float?, current: Float): Boolean =
        previous != null && previous >= config.loopFromThreshold && current <= config.loopToThreshold

    private fun swipe(reason: Reason): Decision {
        swiped = true
        return Decision.SwipeNow(reason)
    }
}

data class ScrollConfig(
    /** Time per video when no progress can be read. */
    val fallbackDelayMs: Long = 15_000,
    /** Swipe when the video is at least this far through. */
    val endThreshold: Float = 0.97f,
    /** A loop: the reading was at least this far through ... */
    val loopFromThreshold: Float = 0.8f,
    /** ... and is now at most this far: back at the start. */
    val loopToThreshold: Float = 0.15f,
    /** With no progress reading for this long, the fallback delay takes over. Above VideoState.SAMPLE_MS. */
    val progressTimeoutMs: Long = 4_500,
    /** Ignore end and loop signals for this long after a video starts. */
    val minWatchMs: Long = 1_000,
)
