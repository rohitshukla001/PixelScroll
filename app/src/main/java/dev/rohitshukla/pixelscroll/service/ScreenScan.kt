package dev.rohitshukla.pixelscroll.service

import android.graphics.Rect
import android.view.accessibility.AccessibilityNodeInfo

/**
 * One pass over the active window: its view ids (for the screen check), YouTube's seek bar
 * (length text and bounds) and progress bar bounds (docs/progress-detection-test.md).
 */
class ScreenScan private constructor(
    val packageName: String,
    val windowBounds: Rect,
    val viewIds: Set<String>,
    val seekDescription: CharSequence?,
    val seekBounds: Rect?,
    val progressBarBounds: Rect?,
) {
    val snapshot get() = ScreenSnapshot(packageName, viewIds)

    companion object {
        private const val YT_PROGRESS_BAR = "com.google.android.youtube:id/reel_progress_bar"
        private const val YT_TIME_BAR = "com.google.android.youtube:id/reel_time_bar"

        fun of(root: AccessibilityNodeInfo): ScreenScan? {
            val pkg = root.packageName?.toString() ?: return null
            val ids = HashSet<String>()
            var seekDesc: CharSequence? = null
            var seekBounds: Rect? = null
            var barBounds: Rect? = null

            fun walk(node: AccessibilityNodeInfo, depth: Int, inTimeBar: Boolean) {
                if (depth > 60) return
                val id = node.viewIdResourceName
                id?.let(ids::add)
                val cls = node.className?.toString().orEmpty()
                // The seek bar sits inside YouTube's time bar; its text follows the phone's
                // language. The pager keeps the next and previous Shorts attached off screen:
                // only the visible seek bar describes the video playing now.
                if (seekDesc == null && cls.endsWith("SeekBar") && node.isVisibleToUser &&
                    !node.contentDescription.isNullOrBlank() &&
                    (inTimeBar || node.contentDescription?.contains(" of ") == true)
                ) {
                    seekDesc = node.contentDescription
                    seekBounds = Rect().also(node::getBoundsInScreen)
                }
                if (barBounds == null && node.isVisibleToUser && id == YT_PROGRESS_BAR) {
                    barBounds = Rect().also(node::getBoundsInScreen)
                }
                val childInTimeBar = inTimeBar || id == YT_TIME_BAR
                for (i in 0 until node.childCount) node.getChild(i)?.let { walk(it, depth + 1, childInTimeBar) }
            }
            walk(root, 0, inTimeBar = false)
            return ScreenScan(pkg, Rect().also(root::getBoundsInScreen), ids, seekDesc, seekBounds, barBounds)
        }
    }
}
