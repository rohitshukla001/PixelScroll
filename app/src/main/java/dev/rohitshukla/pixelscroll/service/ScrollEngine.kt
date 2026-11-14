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
 * 2. reads the Short's length from the seek bar and its progress from a screenshot of the
 *    progress bar (Android 11+): every 2 s, and every second near the end;
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
    private val markerIds = TargetScreens.all.flatMap { it.markerViewIds }
    private val swipe = SwipeDispatcher(service)
    private val decider = ScrollDecider()
    private val median = MedianOfThree()
    private val video = VideoState()

    private var settings = Settings(enabled = false)
    private var running = false

    private var activeApp: TargetApp? = null
    private var activeWindow: Rect? = null
    /** When the pending screenshot was requested, or 0 when none is pending. */
    private var screenshotSentAt = 0L
    private var lastReadAt = 0L
    private var lastReadFailedAt = 0L
    private var lastRawFill: Float? = null
    private var lastRegion: Rect? = null
    private var endSwipeTimed = false
    /** Android refused the last swipe outright; send it again on the next tick. */
    private var swipePending = false
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
        // One search for the Shorts list before walking the whole screen: on YouTube's home
        // feed or a long video, this stops at once.
        if (markerIds.none { root.findAccessibilityNodeInfosByViewId(it).isNotEmpty() }) return deactivate()
        val scan = ScreenScan.of(root) ?: return deactivate()
        val app = detector.detect(scan.snapshot) ?: return deactivate()

        if (activeApp != app) activate(app)
        activeWindow = scan.windowBounds
        val now = SystemClock.elapsedRealtime()

        if (video.onLength(SeekBarDescription.durationSec(scan.seekDescription), now)) onNewVideo()
        if (video.swipeLandedSilently(now)) newVideo()
        decider.config = video.config(settings.fallbackDelaySec)

        if (swipePending) return sendSwipe(retry = true)
        // While screenshots fail after recent good readings, hold the fallback delay so a paused
        // or rewound Short isn't swiped; after READ_FAILURE_HOLD_MS it applies again.
        val readsFailing = lastReadFailedAt > lastReadAt && now - lastReadAt < READ_FAILURE_HOLD_MS
        if (!readsFailing) handle(decider.onTick(now))
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R && video.shouldReadProgress(now)) readProgressBar(scan)
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun readProgressBar(scan: ScreenScan) {
        val now = SystemClock.elapsedRealtime()
        // Wait for the pending screenshot, but not for ever: Android might never answer.
        if (screenshotSentAt != 0L && now - screenshotSentAt < SCREENSHOT_TIMEOUT_MS) return
        val region = barRegion(scan) ?: return
        lastRegion = region
        screenshotSentAt = now
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    screenshotSentAt = 0L
                    try {
                        onScreenshot(result, region)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Screenshot reading failed", e)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    screenshotSentAt = 0L
                    lastReadFailedAt = SystemClock.elapsedRealtime()
                }
            },
        )
    }

    @RequiresApi(Build.VERSION_CODES.R)
    private fun onScreenshot(result: AccessibilityService.ScreenshotResult, region: Rect) {
        val fill = readFill(result, region) ?: run { lastReadFailedAt = SystemClock.elapsedRealtime(); return }
        if (activeApp == null) return
        lastReadAt = SystemClock.elapsedRealtime()
        lastRawFill = fill
        video.onProgressRead(fill, lastReadAt)
        val smooth = median.add(fill) ?: return
        handle(decider.onProgress(smooth, SystemClock.elapsedRealtime()))
        timeEndSwipe(fill, smooth)
    }

    /** Readings come once a second: when the end falls before the next one, swipe right at it. */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun timeEndSwipe(raw: Float, smooth: Float) {
        val lengthMs = video.lengthSec?.times(1000L) ?: return
        if (endSwipeTimed) return
        val delay = EndPredictor.delayToEnd(raw, smooth, lengthMs, TICK_MS) ?: return
        endSwipeTimed = true
        val token = video.token
        handler.postDelayed({ if (token == video.token) confirmThenSwipe(raw, token) }, delay)
    }

    /**
     * Just before the timed swipe, one fresh reading: if the bar went back since [raw], the user
     * rewound, so don't swipe and let the next readings time it again. If no reading can be taken
     * now, swipe anyway: the end is due.
     */
    @RequiresApi(Build.VERSION_CODES.R)
    private fun confirmThenSwipe(raw: Float, token: Int) {
        val latest = lastRawFill
        if (latest != null && latest < raw - REWIND_MARGIN) return run { endSwipeTimed = false }
        val region = lastRegion
        if (region == null || screenshotSentAt != 0L) return swipeAtEnd(token)
        screenshotSentAt = SystemClock.elapsedRealtime()
        service.takeScreenshot(
            Display.DEFAULT_DISPLAY,
            service.mainExecutor,
            object : AccessibilityService.TakeScreenshotCallback {
                override fun onSuccess(result: AccessibilityService.ScreenshotResult) {
                    screenshotSentAt = 0L
                    if (token != video.token) return
                    val fill = try {
                        readFill(result, region)
                    } catch (e: Throwable) {
                        Log.w(TAG, "Screenshot reading failed", e)
                        null
                    }
                    if (fill != null && fill < raw - REWIND_MARGIN) {
                        lastRawFill = fill
                        endSwipeTimed = false
                    } else {
                        swipeAtEnd(token)
                    }
                }

                override fun onFailure(errorCode: Int) {
                    screenshotSentAt = 0L
                    swipeAtEnd(token)
                }
            },
        )
    }

    private fun swipeAtEnd(token: Int) {
        if (token == video.token) handle(decider.onProgress(1f, SystemClock.elapsedRealtime()))
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

    /**
     * Sends the swipe. When Android cancels it, tries once more for the same video. When Android
     * refuses it outright, sends it again on the next tick, without restarting the video's timing.
     */
    private fun sendSwipe(retry: Boolean) {
        val token = video.token
        val accepted = swipe.swipeUp(activeWindow) { completed ->
            if (!completed && retry && token == video.token && activeApp != null) sendSwipe(retry = false)
        }
        swipePending = !accepted
        if (accepted) video.onSwiped(SystemClock.elapsedRealtime())
    }

    private fun activate(app: TargetApp) {
        activeApp = app
        video.start(SystemClock.elapsedRealtime())
        onNewVideo()
    }

    private fun newVideo() {
        video.start(SystemClock.elapsedRealtime())
        onNewVideo()
    }

    /** Resets everything that belongs to one video, after [video] has started a new one. */
    private fun onNewVideo() {
        endSwipeTimed = false
        swipePending = false
        lastRawFill = null
        median.reset()
        decider.onNewVideo(SystemClock.elapsedRealtime())
    }

    private fun deactivate() {
        if (activeApp == null) return
        swipePending = false
        lastRawFill = null
        lastRegion = null
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

        /** Stop waiting for a screenshot answer after this long. */
        const val SCREENSHOT_TIMEOUT_MS = 3_000L

        /** Hold the fallback delay for at most this long while screenshots fail. */
        const val READ_FAILURE_HOLD_MS = 10_000L

        /** A newer reading this much lower than the one a swipe was timed from means a rewind. */
        const val REWIND_MARGIN = 0.05f
    }
}
