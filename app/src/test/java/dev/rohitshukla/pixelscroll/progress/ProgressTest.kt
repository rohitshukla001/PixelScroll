package dev.rohitshukla.pixelscroll.progress

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class ProgressTest {

    private val light = 0xFFDDDDDD.toInt()
    private val track = 0xFF606060.toInt()
    private val video = 0xFF3355AA.toInt()
    private val white = 0xFFFFFFFF.toInt()

    /** A 100 x 20 region: video everywhere, the bar on rows 8..10 filled to [fillPx]. */
    private fun region(fillPx: Int, paint: (IntArray) -> Unit = {}): IntArray {
        val w = 100
        val px = IntArray(w * 20) { video }
        for (y in 8..10) for (x in 0 until w) px[y * w + x] = if (x < fillPx) light else track
        paint(px)
        return px
    }

    @Test
    fun `reads the filled share of the bar`() {
        assertEquals(0.40f, ProgressBarReader.fill(region(40), 100, 20)!!, 0.001f)
        assertEquals(1.00f, ProgressBarReader.fill(region(100), 100, 20)!!, 0.001f)
    }

    @Test
    fun `no light line means no reading`() {
        assertNull(ProgressBarReader.fill(region(0), 100, 20))
    }

    @Test
    fun `bright video content is not mistaken for the bar`() {
        // A white block covering rows 0..19 on the left 70 px: thick, so it is not a thin line.
        val px = region(20) { p -> for (y in 0 until 20) for (x in 0 until 70) p[y * 100 + x] = white }
        val fill = ProgressBarReader.fill(px, 100, 20)
        assertNull(fill)
    }

    @Test
    fun `parses the seek bar description`() {
        assertEquals(45, SeekBarDescription.durationSec("0 minutes 2 seconds of 0 minutes 45 seconds"))
        assertEquals(62, SeekBarDescription.durationSec("0 minutes 0 seconds of 1 minute 2 seconds"))
        assertEquals(30, SeekBarDescription.durationSec("5 seconds of 30 seconds"))
        assertNull(SeekBarDescription.durationSec("Video Progress"))
        assertNull(SeekBarDescription.durationSec(null))
    }

    @Test
    fun `reads the length from the numbers when the phone is not in English`() {
        // Hindi and German style descriptions: words differ, numbers keep their order.
        assertEquals(45, SeekBarDescription.durationSec("0 मिनट 2 सेकंड / 0 मिनट 45 सेकंड"))
        assertEquals(62, SeekBarDescription.durationSec("0 Minuten 0 Sekunden von 1 Minute 2 Sekunden"))
        assertEquals(3_725, SeekBarDescription.durationSec("0 h 0 min 1 s - 1 h 2 min 5 s"))
        assertNull(SeekBarDescription.durationSec("3 Sekunden"))
        assertNull(SeekBarDescription.durationSec("0 Minuten 0 Sekunden von 0 Minuten 0 Sekunden"))
    }

    @Test
    fun `median of three drops a single wrong reading`() {
        val m = MedianOfThree()
        assertNull(m.add(0.10f))
        assertNull(m.add(0.12f))
        assertEquals(0.12f, m.add(0.95f)!!, 0f) // spike ignored
        assertEquals(0.14f, m.add(0.14f)!!, 0f)
        m.reset()
        assertNull(m.add(0.5f))
    }
}
