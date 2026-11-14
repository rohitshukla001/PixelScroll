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
    fun `reads progress only near the end of a Short with a known length`() {
        state.start(0)
        assertTrue(state.shouldReadProgress(0))
        state.onLength(30, 500)
        assertFalse(state.shouldReadProgress(10_000))
        assertFalse(state.shouldReadProgress(24_999))
        assertTrue(state.shouldReadProgress(25_000))
    }

    @Test
    fun `reads progress for the whole first Short after Shorts opens`() {
        state.start(0, readFromStart = true)
        state.onLength(30, 500)
        assertTrue(state.shouldReadProgress(2_000))
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
