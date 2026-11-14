package dev.rohitshukla.pixelscroll.scroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class EndPredictorTest {

    @Test
    fun `waits when the end is more than one reading away`() {
        // 8 s video at 75%: 2 s left.
        assertNull(EndPredictor.delayToEnd(raw = 0.75f, smooth = 0.63f, lengthMs = 8_000, readingIntervalMs = 1_000))
    }

    @Test
    fun `swipes just before the end when it comes before the next reading`() {
        // 8 s video at 89%: 880 ms left, swipe 250 ms early.
        assertEquals(630L, EndPredictor.delayToEnd(raw = 0.89f, smooth = 0.76f, lengthMs = 8_000, readingIntervalMs = 1_000))
    }

    @Test
    fun `swipes now when the end is closer than the swipe lead`() {
        assertEquals(0L, EndPredictor.delayToEnd(raw = 0.99f, smooth = 0.9f, lengthMs = 8_000, readingIntervalMs = 1_000))
    }

    @Test
    fun `ignores a reading early in the video`() {
        assertNull(EndPredictor.delayToEnd(raw = 0.95f, smooth = 0.2f, lengthMs = 8_000, readingIntervalMs = 1_000))
    }

    @Test
    fun `ignores a reading that dropped back, such as a loop`() {
        assertNull(EndPredictor.delayToEnd(raw = 0.02f, smooth = 0.88f, lengthMs = 8_000, readingIntervalMs = 1_000))
    }
}
