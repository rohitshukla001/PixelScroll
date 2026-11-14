package dev.rohitshukla.pixelscroll.scroll

import dev.rohitshukla.pixelscroll.scroll.ScrollDecider.Decision
import dev.rohitshukla.pixelscroll.scroll.ScrollDecider.Reason
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class ScrollDeciderTest {

    private val config = ScrollConfig(
        fallbackDelayMs = 15_000,
        endThreshold = 0.97f,
        loopFromThreshold = 0.8f,
        loopToThreshold = 0.15f,
        progressTimeoutMs = 3_000,
        minWatchMs = 1_000,
    )
    private lateinit var decider: ScrollDecider

    @Before
    fun setUp() {
        decider = ScrollDecider(config)
    }

    @Test
    fun `waits before any video has started`() {
        assertEquals(Decision.Wait, decider.onProgress(0.99f, 5_000))
        assertEquals(Decision.Wait, decider.onTick(60_000))
    }

    @Test
    fun `swipes when the video reaches its end`() {
        decider.onNewVideo(0)
        assertEquals(Decision.Wait, decider.onProgress(0.5f, 3_500))
        assertEquals(Decision.Wait, decider.onProgress(0.9f, 6_300))
        assertEquals(Decision.SwipeNow(Reason.END_REACHED), decider.onProgress(0.98f, 6_900))
    }

    @Test
    fun `swipes when the video loops back to the start`() {
        decider.onNewVideo(0)
        decider.onProgress(0.85f, 6_000)
        assertEquals(Decision.SwipeNow(Reason.LOOPED), decider.onProgress(0.05f, 7_000))
    }

    @Test
    fun `a paused video is never swiped`() {
        decider.onNewVideo(0)
        var now = 0L
        repeat(120) {
            now += 500
            assertEquals(Decision.Wait, decider.onProgress(0.4f, now))
            assertEquals(Decision.Wait, decider.onTick(now))
        }
    }

    @Test
    fun `falls back to the fixed delay when no progress is read`() {
        decider.onNewVideo(0)
        assertEquals(Decision.Wait, decider.onTick(14_999))
        assertEquals(Decision.SwipeNow(Reason.FALLBACK_DELAY), decider.onTick(15_000))
    }

    @Test
    fun `falls back to the fixed delay when progress stops arriving`() {
        decider.onNewVideo(0)
        decider.onProgress(0.3f, 4_000)
        assertEquals(Decision.Wait, decider.onTick(6_999))
        assertEquals(Decision.Wait, decider.onTick(14_000))
        assertEquals(Decision.SwipeNow(Reason.FALLBACK_DELAY), decider.onTick(15_000))
    }

    @Test
    fun `swipes only once per video`() {
        decider.onNewVideo(0)
        assertEquals(Decision.SwipeNow(Reason.END_REACHED), decider.onProgress(0.99f, 7_000))
        assertEquals(Decision.Wait, decider.onProgress(0.01f, 7_500))
        assertEquals(Decision.Wait, decider.onTick(60_000))
    }

    @Test
    fun `a new video after a manual swipe is not mistaken for a loop`() {
        decider.onNewVideo(0)
        decider.onProgress(0.8f, 5_000)
        decider.onNewVideo(5_200)
        assertEquals(Decision.Wait, decider.onProgress(0.02f, 6_300))
        assertEquals(Decision.Wait, decider.onTick(6_300))
    }

    @Test
    fun `ignores end and loop signals right after a video starts`() {
        decider.onNewVideo(10_000)
        assertEquals(Decision.Wait, decider.onProgress(0.99f, 10_300))
        assertEquals(Decision.SwipeNow(Reason.END_REACHED), decider.onProgress(0.99f, 11_000))
    }

    @Test
    fun `clamps readings outside 0 to 1 and ignores NaN`() {
        decider.onNewVideo(0)
        assertEquals(Decision.Wait, decider.onProgress(Float.NaN, 2_000))
        assertEquals(Decision.Wait, decider.onProgress(-0.5f, 2_500))
        assertEquals(Decision.SwipeNow(Reason.END_REACHED), decider.onProgress(1.4f, 3_000))
    }

    @Test
    fun `waits after stop until a new video starts`() {
        decider.onNewVideo(0)
        decider.stop()
        assertEquals(Decision.Wait, decider.onProgress(0.99f, 5_000))
        assertEquals(Decision.Wait, decider.onTick(60_000))
        decider.onNewVideo(61_000)
        assertEquals(Decision.SwipeNow(Reason.FALLBACK_DELAY), decider.onTick(76_000))
    }

    @Test
    fun `a rewind to the middle is not a loop`() {
        decider.onNewVideo(0)
        decider.onProgress(0.9f, 20_000)
        assertEquals(Decision.Wait, decider.onProgress(0.4f, 21_000))
        assertEquals(Decision.Wait, decider.onTick(21_000))
    }

    @Test
    fun `a rewound Short still swipes at its real end`() {
        decider.onNewVideo(0)
        decider.onProgress(0.9f, 20_000)
        decider.onProgress(0.4f, 21_000)
        assertEquals(Decision.Wait, decider.onProgress(0.7f, 23_000))
        assertEquals(Decision.SwipeNow(Reason.END_REACHED), decider.onProgress(0.98f, 25_000))
    }

    @Test
    fun `a jump forward swipes at the end, not after a replay`() {
        decider.onNewVideo(0)
        decider.onProgress(0.1f, 2_000)
        assertEquals(Decision.SwipeNow(Reason.END_REACHED), decider.onProgress(0.99f, 4_000))
    }
}
