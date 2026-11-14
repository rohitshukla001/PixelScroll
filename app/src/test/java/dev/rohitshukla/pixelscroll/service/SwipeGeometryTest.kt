package dev.rohitshukla.pixelscroll.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class SwipeGeometryTest {

    @Test
    fun `swipes up through the middle from 80 to 20 percent of the height`() {
        assertEquals(SwipeLine(x = 540f, startY = 1920f, endY = 480f), SwipeGeometry.upward(0, 0, 1080, 2400))
    }

    @Test
    fun `stays inside an area that does not start at the screen corner`() {
        // Bottom half of a split screen: 1080 x 1200, starting at y = 1200.
        assertEquals(SwipeLine(x = 540f, startY = 2160f, endY = 1440f), SwipeGeometry.upward(0, 1200, 1080, 1200))
    }

    @Test
    fun `returns null for an empty area`() {
        assertNull(SwipeGeometry.upward(0, 0, 0, 2400))
        assertNull(SwipeGeometry.upward(0, 0, 1080, -1))
    }
}
