package dev.rohitshukla.pixelscroll

import android.content.Intent
import android.database.ContentObserver
import android.graphics.Color
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.provider.Settings as AndroidSettings
import androidx.activity.ComponentActivity
import androidx.activity.SystemBarStyle
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.lifecycle.lifecycleScope
import dev.rohitshukla.pixelscroll.data.AppData
import dev.rohitshukla.pixelscroll.data.Settings
import dev.rohitshukla.pixelscroll.data.SetupState
import dev.rohitshukla.pixelscroll.service.ServiceStatus
import dev.rohitshukla.pixelscroll.ui.AppController
import dev.rohitshukla.pixelscroll.ui.PixelScrollApp
import dev.rohitshukla.pixelscroll.ui.theme.PixelScrollTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity(), AppController {

    private val settingsRepo by lazy { AppData.settings(this) }
    private val setupState by lazy { SetupState(this) }

    /** On in Android's accessibility settings. Running is checked separately. */
    private var serviceEnabled by mutableStateOf(false)

    /** Follows the accessibility setting while the app is open, not only when it comes back. */
    private val enabledObserver = object : ContentObserver(Handler(Looper.getMainLooper())) {
        override fun onChange(selfChange: Boolean) {
            serviceEnabled = ServiceStatus.isEnabled(this@MainActivity)
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Light app: dark status and navigation bar icons, also when the phone is in dark mode.
        enableEdgeToEdge(
            statusBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
            navigationBarStyle = SystemBarStyle.light(Color.TRANSPARENT, Color.TRANSPARENT),
        )
        serviceEnabled = ServiceStatus.isEnabled(this)
        contentResolver.registerContentObserver(
            AndroidSettings.Secure.getUriFor(AndroidSettings.Secure.ENABLED_ACCESSIBILITY_SERVICES),
            false,
            enabledObserver,
        )
        setContent {
            val settings by settingsRepo.settings.collectAsState(initial = Settings())
            val serviceRunning by ServiceStatus.running.collectAsState()
            PixelScrollTheme {
                PixelScrollApp(
                    setupComplete = setupState.isComplete,
                    serviceEnabled = serviceEnabled,
                    serviceRunning = serviceRunning,
                    settings = settings,
                    controller = this,
                )
            }
        }
    }

    override fun onDestroy() {
        contentResolver.unregisterContentObserver(enabledObserver)
        super.onDestroy()
    }

    override fun onResume() {
        super.onResume()
        serviceEnabled = ServiceStatus.isEnabled(this)
    }

    override fun finishSetup() {
        setupState.isComplete = true
    }

    override fun setEnabled(on: Boolean) {
        lifecycleScope.launch { settingsRepo.setEnabled(on) }
    }

    override fun setFallbackDelay(seconds: Int) {
        lifecycleScope.launch { settingsRepo.setFallbackDelay(seconds) }
    }

    override fun openAccessibilitySettings() {
        startActivity(Intent(AndroidSettings.ACTION_ACCESSIBILITY_SETTINGS))
    }
}
