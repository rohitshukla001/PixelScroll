package dev.rohitshukla.pixelscroll.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.util.Log

/** Sends the upward swipe that moves Shorts to the next video. */
class SwipeDispatcher(private val service: AccessibilityService) {

    /**
     * Swipes up through the middle of [area] (the target app's window), or of the whole display
     * when [area] is null or empty. [onResult] gets true when the gesture finished, false when
     * Android cancelled it. Returns false when Android refused the gesture outright.
     */
    fun swipeUp(area: Rect? = null, onResult: (completed: Boolean) -> Unit = {}): Boolean {
        val metrics = service.resources.displayMetrics
        val line = area?.takeIf { !it.isEmpty }
            ?.let { SwipeGeometry.upward(it.left, it.top, it.width(), it.height()) }
            ?: SwipeGeometry.upward(0, 0, metrics.widthPixels, metrics.heightPixels)
            ?: return false

        val path = Path().apply {
            moveTo(line.x, line.startY)
            lineTo(line.x, line.endY)
        }
        val gesture = GestureDescription.Builder()
            .addStroke(GestureDescription.StrokeDescription(path, 0, SWIPE_DURATION_MS))
            .build()

        return service.dispatchGesture(gesture, object : AccessibilityService.GestureResultCallback() {
            override fun onCompleted(gestureDescription: GestureDescription?) {
                Log.d(TAG, "Swipe completed")
                onResult(true)
            }

            override fun onCancelled(gestureDescription: GestureDescription?) {
                Log.w(TAG, "Swipe cancelled")
                onResult(false)
            }
        }, null)
    }

    private companion object {
        const val TAG = "SwipeDispatcher"

        /** Fast enough to count as a fling, so the video pager moves one page. */
        const val SWIPE_DURATION_MS = 250L
    }
}
