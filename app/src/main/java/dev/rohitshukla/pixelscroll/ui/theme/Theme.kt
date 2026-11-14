package dev.rohitshukla.pixelscroll.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.Immutable
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp

/** Colour tokens from the Pixel Scroll design system (tokens.json). */
@Immutable
data class PsColors(
    val background: Color,
    val surface: Color,
    val surfaceContainer: Color,
    val primary: Color,
    val onPrimary: Color,
    val secondary: Color,
    val textPrimary: Color,
    val textSecondary: Color,
    val outline: Color,
    val divider: Color,
    val disabled: Color,
    val success: Color,
    val error: Color,
)

val LightColors = PsColors(
    background = Color(0xFFFAFAFA),
    surface = Color(0xFFFFFFFF),
    surfaceContainer = Color(0xFFF4F4F5),
    primary = Color(0xFF09090B),
    onPrimary = Color(0xFFFFFFFF),
    secondary = Color(0xFF52525B),
    textPrimary = Color(0xFF09090B),
    textSecondary = Color(0xFF71717A),
    outline = Color(0xFFE4E4E7),
    divider = Color(0xFFF4F4F5),
    disabled = Color(0xFFD4D4D8),
    success = Color(0xFF15803D),
    error = Color(0xFFB91C1C),
)

/** Spacing (8 dp grid), radius and size tokens. */
object Space {
    val s1 = 4.dp
    val s2 = 8.dp
    val s3 = 12.dp
    val s4 = 16.dp
    val s5 = 20.dp
    val s6 = 24.dp
    val s10 = 40.dp
}

object Radius {
    val md = 12.dp
    val lg = 16.dp
    val xl = 24.dp
}

object Size {
    val tap = 48.dp
    val button = 56.dp
}

private val LocalPsColors = staticCompositionLocalOf { LightColors }

object Ps {
    val colors: PsColors @Composable get() = LocalPsColors.current
}

/** Light theme only, whatever the phone's setting. Material is mapped too, for ripples and the slider. */
@Composable
fun PixelScrollTheme(content: @Composable () -> Unit) {
    val c = LightColors
    val scheme = lightColorScheme(
        primary = c.primary,
        onPrimary = c.onPrimary,
        background = c.background,
        onBackground = c.textPrimary,
        surface = c.surface,
        onSurface = c.textPrimary,
        onSurfaceVariant = c.textSecondary,
        outline = c.outline,
        error = c.error,
    )
    CompositionLocalProvider(LocalPsColors provides c) {
        MaterialTheme(colorScheme = scheme, typography = PsTypography, content = content)
    }
}
