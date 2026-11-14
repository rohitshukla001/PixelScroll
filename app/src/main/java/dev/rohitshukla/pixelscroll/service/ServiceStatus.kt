package dev.rohitshukla.pixelscroll.service

import android.content.ComponentName
import android.content.Context
import android.provider.Settings
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

object ServiceStatus {
    private val runningState = MutableStateFlow(false)

    /**
     * True while the service is connected and running in this process. Android can list the
     * service as on while it is not running (after a crash, or when the phone stops it).
     */
    val running: StateFlow<Boolean> = runningState

    internal fun setRunning(on: Boolean) {
        runningState.value = on
    }

    /** True when Pixel Scroll is in Android's list of enabled accessibility services. */
    fun isEnabled(context: Context): Boolean {
        val ours = ComponentName(context, PixelScrollAccessibilityService::class.java)
        val enabled = Settings.Secure.getString(context.contentResolver, Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES)
            ?: return false
        return enabled.split(':').any { ComponentName.unflattenFromString(it) == ours }
    }
}
