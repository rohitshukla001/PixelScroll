package dev.rohitshukla.pixelscroll.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class TargetScreenDetectorTest {

    private val youtube = "com.google.android.youtube"
    private val shortsMarker = "$youtube:id/shorts_marker"

    private val detector = TargetScreenDetector(
        listOf(TargetScreen(TargetApp.YOUTUBE_SHORTS, youtube, setOf(shortsMarker))),
    )

    @Test
    fun `detects the Shorts screen`() {
        val snapshot = ScreenSnapshot(youtube, setOf("$youtube:id/toolbar", shortsMarker))
        assertEquals(TargetApp.YOUTUBE_SHORTS, detector.detect(snapshot))
    }

    @Test
    fun `ignores a normal YouTube video`() {
        val snapshot = ScreenSnapshot(youtube, setOf("$youtube:id/watch_player", "$youtube:id/toolbar"))
        assertNull(detector.detect(snapshot))
    }

    @Test
    fun `ignores Instagram and other apps`() {
        val instagram = "com.instagram.android"
        assertNull(detector.detect(ScreenSnapshot(instagram, setOf("$instagram:id/clips_viewer_view_pager"))))
        assertNull(detector.detect(ScreenSnapshot("com.android.launcher3", emptySet())))
        assertNull(detector.detect(ScreenSnapshot("com.whatsapp", setOf("com.whatsapp:id/conversation"))))
    }

    @Test
    fun `needs the exact package name, not a similar one`() {
        val snapshot = ScreenSnapshot("com.google.android.apps.youtube.music", setOf(shortsMarker))
        assertNull(detector.detect(snapshot))
    }

    @Test
    fun `a marker only counts in its own app`() {
        assertNull(detector.detect(ScreenSnapshot("com.instagram.android", setOf(shortsMarker))))
    }

    @Test
    fun `the real config covers YouTube Shorts only, with at least one marker`() {
        assertEquals(listOf(TargetApp.YOUTUBE_SHORTS), TargetScreens.all.map { it.app })
        TargetScreens.all.forEach { target ->
            assertTrue("${target.app} has no marker ids", target.markerViewIds.isNotEmpty())
            target.markerViewIds.forEach { id ->
                assertTrue("$id does not belong to ${target.packageName}", id.startsWith("${target.packageName}:id/"))
            }
        }
    }
}
