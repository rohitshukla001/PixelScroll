package dev.rohitshukla.pixelscroll.service

import android.accessibilityservice.AccessibilityService
import android.content.Intent
import android.view.accessibility.AccessibilityEvent
import dev.rohitshukla.pixelscroll.data.AppData
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

/** Pixel Scroll's accessibility service. It runs the [ScrollEngine] with the current settings. */
class PixelScrollAccessibilityService : AccessibilityService() {

    private var scope: CoroutineScope? = null
    private var engine: ScrollEngine? = null

    override fun onServiceConnected() {
        super.onServiceConnected()
        val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate).also { scope = it }
        val engine = ScrollEngine(this, onHealthChanged = ServiceStatus::setRunning).also { engine = it }
        scope.launch {
            AppData.settings(this@PixelScrollAccessibilityService).settings.collect(engine::updateSettings)
        }
        ServiceStatus.setRunning(true)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        if (event.eventType != AccessibilityEvent.TYPE_VIEW_SCROLLED) return
        val pkg = event.packageName?.toString() ?: return
        engine?.onScrolled(pkg, event.source?.viewIdResourceName)
    }

    override fun onInterrupt() = Unit

    override fun onUnbind(intent: Intent?): Boolean {
        release()
        return super.onUnbind(intent)
    }

    override fun onDestroy() {
        release()
        super.onDestroy()
    }

    private fun release() {
        ServiceStatus.setRunning(false)
        engine?.shutdown()
        engine = null
        scope?.cancel()
        scope = null
    }
}
