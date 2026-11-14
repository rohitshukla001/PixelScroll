package dev.rohitshukla.pixelscroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Rect
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.PowerManager
import android.os.SystemClock
import android.util.Log
import android.view.Display
import androidx.annotation.RequiresApi
import dev.rohitshukla.pixelscroll.data.Settings
import dev.rohitshukla.pixelscroll.progress.MedianOfThree
import dev.rohitshukla.pixelscroll.progress.ProgressBarReader
import dev.rohitshukla.pixelscroll.progress.SeekBarDescription
import dev.rohitshukla.pixelscroll.scroll.EndPredictor
import dev.rohitshukla.pixelscroll.scroll.ScrollDecider
import dev.rohitshukla.pixelscroll.scroll.VideoState

/**
 * The scroll engine. Once a second, while auto-scroll is on and the screen is on:
 * 1. checks the app in front is YouTube and shows Shorts;
 * 2. reads the Short's length from the seek bar and, near the end, its progress from a
 *    screenshot of the progress bar (Android 11+);
 * 3. asks [ScrollDecider] whether to swipe, and swipes in YouTube's window.
 * A Short whose length can't be read uses the fallback delay.
 *
 * [onHealthChanged] reports false after several seconds in a row fail with an error, and
 * true again once a second runs cleanly.
 */
class ScrollEngine(
    private val service: AccessibilityService,
    private val onHealthChanged: (healthy: Boolean) -> Unit,
) {
    private val handler = Handler(Looper.getMainLooper())
    private val power = service.getSystemService(Context.POWER_SERVICE) as PowerManager
    private val detector = TargetScreenDetector()
    private val targetPackages = TargetScreens.all.map { it.packageName }.toSet()
    private val swipe = SwipeDispatcher(service)
    private val decider = ScrollDecider()
    private val median = MedianOfThree()
    private val video = VideoState()

    private var settings = Settings(enabled = false)
    private var running = false

    private var activeApp: TargetApp? = null
    private var activeWindow: Rect? = null
    private var screenshotBusy = false
    private var endSwipeTimed = false
    private var failedTicks = 0

    fun updateSettings(new: Settings) {
        settings = new
        if (new.enabled && !running) start()
        if (!new.enabled && running) stop()
    }

    /** A scroll event in the target app: a new video is on screen (the user's swipe or ours). */
    fun onScrolled(packageName: String, sourceViewId: String?) {
        val app = activeApp ?: return
        val target = TargetScreens.all.first { it.app == app }
        if (packageName != target.packageName) return
        // Only the Shorts pager itself: other lists on screen (comments, a scrolling
        // title) and events without a view id would restart the timer on every scroll.
        if (sourceViewId !in target.markerViewIds) return
        newVideo()
    }

    fun shutdown() {
        stop()
    }

    private fun start() {
        running = true
        handler.post(::tick)
    }

    private fun stop() {
        running = false
        handler.removeCallbacksAndMessages(null)
        deactivate()
    }

    private fun tick() {
        if (!running) return
        try {
            step()
            if (failedTicks >= MAX_FAILED_TICKS) onHealthChanged(true)
            failedTicks = 0
        } catch (e: Throwable) {
            // A thrown error here would end the service's process, and with it auto-scroll,
            // while Android still lists the service as on. Skip this second instead, and report
            // it when it keeps happening.
            Log.w(TAG, "Tick failed", e)
            if (++failedTicks == MAX_FAILED_TICKS) onHealthChanged(false)
        } finally {
            handler.postDelayed(::tick, TICK_MS)
        }
    }

    private fun step() {
        if (!power.isInteractive) return deactivate()
        val root = service.rootInActiveWindow ?: return deactivate()
        // Look inside YouTube only: never walk the screens of other apps.
        if (root.packageName?.toString() !in targetPackages) return deactivate()
        val scan = ScreenScan.of(root) ?: return deactivate()
        val app = detector.detect(scan.snapshot) ?: return deactivate()

        if (activeApp != app) activate(app)
        activeWindow = scan.windowBounds
        val now = SystemClock.elapsedRealtime()

        if (video.onLength(SeekBarDescription.durationSec(scan.seekDescription), now)) onNewVideo()
        if (video.swipeLandedSilently(now)) newVideo()
        decider.config = video.config(settings.fallbackDelaySec)

        handle(decider.onTick(now))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && video.shouldReadProgress(now)) readProgressBar(scan)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readProgressBar(scan: ScreenScan) {
        if (screenshotBusy) return
        val region = barRegion(scan) ?: return
        screenshotBusy = true
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    screenshotBusy = false
                    try {
                        onScreenshot(result, region)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Screenshot reading failed", e)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    screenshotBusy = false
                }
            },
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onScreenshot(result: AccessibilityService.ScreenshotResult, region: Rect) {
        val fill = readFill(result, region) ?: return
        if (activeApp == null) return
        val smooth = median.add(fill) ?: return
        handle(decider.onProgress(smooth, SystemClock.elapsedRealtime()))
        timeEndSwipe(fill, smooth)
    }

    /** Readings come once a second: when the end falls before the next one, swipe right at it. */
    private fun timeEndSwipe(raw: Float, smooth: Float) {
        val lengthMs = video.lengthSec?.times(1000L) ?: return
        if (endSwipeTimed) return
        val delay = EndPredictor.delayToEnd(raw, smooth, lengthMs, TICK_MS) ?: return
        endSwipeTimed = true
        val token = video.token
        handler.postDelayed({
            if (token == video.token) handle(decider.onProgress(1f, SystemClock.elapsedRealtime()))
        }, delay)
    }

    /** Where to look for the bar: its own node, else the middle band of the seek bar. */
    private fun barRegion(scan: ScreenScan): Rect? {
        scan.progressBarBounds?.takeIf { !it.isEmpty }?.let { bar ->
            return Rect(bar.left, bar.top - 8, bar.right, bar.bottom + 8)
        }
        val seek = scan.seekBounds?.takeIf { !it.isEmpty } ?: return null
        val inset = seek.width() / 50
        return Rect(seek.left + inset, seek.top + seek.height() * 3 / 10, seek.right - inset, seek.top + seek.height() * 7 / 10)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readFill(result: AccessibilityService.ScreenshotResult, region: Rect): Float? {
        val buffer = result.hardwareBuffer
        val hardware = Bitmap.wrapHardwareBuffer(buffer, result.colorSpace)
        buffer.close()
        hardware ?: return null
        // copy() returns null when it can't allocate the full-screen bitmap.
        val soft: Bitmap? = hardware.copy(Bitmap.Config.ARGB_8888, false)
        hardware.recycle()
        soft ?: return null
        try {
            val r = Rect(region)
            if (!r.intersect(0, 0, soft.width, soft.height) || r.width() < 10 || r.height() < 3) return null
            val pixels = IntArray(r.width() * r.height())
            soft.getPixels(pixels, 0, r.width(), r.left, r.top, r.width(), r.height())
            return ProgressBarReader.fill(pixels, r.width(), r.height())
        } finally {
            soft.recycle()
        }
    }

    private fun handle(decision: ScrollDecider.Decision) {
        if (decision !is ScrollDecider.Decision.SwipeNow || activeApp == null) return
        sendSwipe(retry = true)
    }

    /** Sends the swipe; when Android cancels it, tries once more for the same video. */
    private fun sendSwipe(retry: Boolean) {
        val token = video.token
        video.onSwiped(SystemClock.elapsedRealtime())
        swipe.swipeUp(activeWindow) { completed ->
            if (!completed && retry && token == video.token && activeApp != null) sendSwipe(retry = false)
        }
    }

    private fun activate(app: TargetApp) {
        activeApp = app
        video.start(SystemClock.elapsedRealtime(), readFromStart = true)
        onNewVideo()
    }

    private fun newVideo() {
        video.start(SystemClock.elapsedRealtime())
        onNewVideo()
    }

    /** Resets everything that belongs to one video, after [video] has started a new one. */
    private fun onNewVideo() {
        endSwipeTimed = false
        median.reset()
        decider.onNewVideo(SystemClock.elapsedRealtime())
    }

    private fun deactivate() {
        if (activeApp == null) return
        video.clear()
        activeApp = null
        activeWindow = null
        decider.stop()
        median.reset()
    }

    private companion object {
        const val TAG = "ScrollEngine"
        const val TICK_MS = 1_000L

        /** After this many failed seconds in a row, report the engine as not running. */
        const val MAX_FAILED_TICKS = 5
    }
}
