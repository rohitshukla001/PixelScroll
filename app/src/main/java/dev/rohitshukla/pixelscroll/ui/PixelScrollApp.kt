package dev.rohitshukla.pixelscroll.ui

import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import dev.rohitshukla.pixelscroll.data.Settings
import dev.rohitshukla.pixelscroll.ui.screens.AccessScreen
import dev.rohitshukla.pixelscroll.ui.screens.HomeScreen
import dev.rohitshukla.pixelscroll.ui.screens.WelcomeScreen
import dev.rohitshukla.pixelscroll.ui.theme.Ps

/** Everything the screens ask the app to do. MainActivity implements it. */
interface AppController {
    fun finishSetup()
    fun setEnabled(on: Boolean)
    fun setFallbackDelay(seconds: Int)
    fun openAccessibilitySettings()
}

enum class Screen { Welcome, Access, Home }

/** Three screens. First run: Welcome, then Accessibility access. After that: Home. */
@Composable
fun PixelScrollApp(
    setupComplete: Boolean,
    serviceEnabled: Boolean,
    serviceRunning: Boolean,
    settings: Settings,
    controller: AppController,
) {
    val serviceConnected = serviceEnabled && serviceRunning
    var screen by rememberSaveable { mutableStateOf(if (setupComplete) Screen.Home else Screen.Welcome) }
    // Where Back from Accessibility access goes: Welcome during first run, else Home.
    var accessBack by rememberSaveable { mutableStateOf(Screen.Welcome) }

    fun goHome() {
        controller.finishSetup()
        screen = Screen.Home
    }

    fun openAccess(from: Screen) {
        accessBack = from
        screen = Screen.Access
    }

    // Back from Android's settings with the service on: setup is done.
    LaunchedEffect(screen, serviceConnected) {
        if (screen == Screen.Access && serviceConnected) goHome()
    }

    BackHandler(enabled = screen == Screen.Access) { screen = accessBack }

    Box(Modifier.fillMaxSize().background(Ps.colors.background)) {
        AnimatedContent(targetState = screen, transitionSpec = { fadeIn() togetherWith fadeOut() }, label = "screen") { current ->
            when (current) {
                Screen.Welcome -> WelcomeScreen(onGetStarted = { openAccess(from = Screen.Welcome) })
                Screen.Access -> AccessScreen(
                    onBack = { screen = accessBack },
                    onOpenSettings = controller::openAccessibilitySettings,
                    onNotNow = ::goHome,
                )
                Screen.Home -> HomeScreen(
                    settings = settings,
                    serviceConnected = serviceConnected,
                    serviceEnabled = serviceEnabled,
                    onToggle = controller::setEnabled,
                    onDelayChange = controller::setFallbackDelay,
                    onFix = { openAccess(from = Screen.Home) },
                )
            }
        }
    }
}
