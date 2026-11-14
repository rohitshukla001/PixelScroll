package dev.rohitshukla.pixelscroll.scroll

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class VideoStateTest {

    private val state = VideoState()

    @Test
    fun `a new video forgets the previous length`() {
        state.start(0)
        state.onLength(45, 1_000)
        state.start(50_000)
        assertNull(state.lengthSec)
        assertEquals(15_000L, state.config(fallbackDelaySec = 15).fallbackDelayMs)
    }

    @Test
    fun `a different length means a new Short`() {
        state.start(0)
        assertFalse(state.onLength(30, 1_000))
        val token = state.token
        assertFalse(state.onLength(30, 2_000))
        assertTrue(state.onLength(12, 3_000))
        assertEquals(12, state.lengthSec)
        assertNotEquals(token, state.token)
    }

    @Test
    fun `a missing length keeps the known one`() {
        state.start(0)
        state.onLength(30, 1_000)
        assertFalse(state.onLength(null, 2_000))
        assertEquals(30, state.lengthSec)
    }

    @Test
    fun `the fallback is the length plus one second when the length is known`() {
        state.start(0)
        state.onLength(30, 1_000)
        assertEquals(31_000L, state.config(fallbackDelaySec = 15).fallbackDelayMs)
    }

    @Test
    fun `reads progress every 3 s mid-Short and every second near the end`() {
        state.start(0)
        assertTrue(state.shouldReadProgress(0))
        state.onLength(30, 0)
        state.onProgressRead(0.2f, 1_000) // 24 s left
        assertFalse(state.shouldReadProgress(3_000))
        assertTrue(state.shouldReadProgress(4_000))
        state.onProgressRead(0.85f, 3_000) // 4.5 s left: inside the last 5 s
        assertTrue(state.shouldReadProgress(4_000))
    }

    @Test
    fun `a jump forward moves the reading window to the real end`() {
        state.start(0)
        state.onLength(60, 0)
        state.onProgressRead(0.1f, 2_000)
        // The user jumps to 95%: the next sample sees it and reading goes every second.
        state.onProgressRead(0.95f, 4_000)
        assertTrue(state.shouldReadProgress(5_000))
    }

    @Test
    fun `a jump back moves the reading window later`() {
        state.start(0)
        state.onLength(30, 0)
        state.onProgressRead(0.9f, 26_000) // near the end: every second
        assertTrue(state.shouldReadProgress(27_000))
        state.onProgressRead(0.3f, 27_000) // rewound to 30%: back to every 3 s
        assertFalse(state.shouldReadProgress(29_000))
        assertTrue(state.shouldReadProgress(30_000))
    }

    @Test
    fun `a reading that lands just after its tick still reads on the tick 3 s later`() {
        state.start(0)
        state.onLength(30, 0)
        state.onProgressRead(0.2f, 1_030)
        assertTrue(state.shouldReadProgress(4_000))
    }

    @Test
    fun `a swipe with no scroll event counts as landed after 3 seconds`() {
        state.start(0)
        assertFalse(state.swipeLandedSilently(10_000))
        state.onSwiped(10_000)
        assertFalse(state.swipeLandedSilently(13_000))
        assertTrue(state.swipeLandedSilently(13_001))
        state.start(13_001)
        assertFalse(state.swipeLandedSilently(20_000))
    }

    @Test
    fun `clear changes the token and forgets the length`() {
        state.start(0)
        state.onLength(30, 500)
        val token = state.token
        state.clear()
        assertNull(state.lengthSec)
        assertNotEquals(token, state.token)
    }
}
