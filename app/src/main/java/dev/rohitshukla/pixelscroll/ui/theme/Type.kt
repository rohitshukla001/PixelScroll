package dev.rohitshukla.pixelscroll.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.em
import androidx.compose.ui.unit.sp
import dev.rohitshukla.pixelscroll.R

val Inter = FontFamily(
    Font(R.font.inter_regular, FontWeight.Normal),
    Font(R.font.inter_medium, FontWeight.Medium),
    Font(R.font.inter_semibold, FontWeight.SemiBold),
)

private fun style(size: Int, line: Int, weight: FontWeight, tracking: Double = 0.0) = TextStyle(
    fontFamily = Inter,
    fontSize = size.sp,
    lineHeight = line.sp,
    fontWeight = weight,
    letterSpacing = tracking.em,
)

/** Type styles from the design system's tokens.json, one per role. */
object PsType {
    val display = style(32, 40, FontWeight.SemiBold, -0.02)
    val displaySm = style(28, 36, FontWeight.SemiBold, -0.02)
    val headline = style(24, 32, FontWeight.SemiBold, -0.01)
    val title = style(18, 24, FontWeight.Medium)
    val titleStrong = style(18, 24, FontWeight.SemiBold)
    val titleSm = style(16, 24, FontWeight.Medium)
    val bodyLg = style(16, 24, FontWeight.Normal)
    val body = style(14, 20, FontWeight.Normal)
    val label = style(14, 20, FontWeight.Medium)
    val button = style(16, 24, FontWeight.Medium)
    val caption = style(12, 16, FontWeight.Normal)
    val overline = style(12, 16, FontWeight.Medium, 0.08)
    val value = style(18, 24, FontWeight.SemiBold).copy(fontFeatureSettings = "tnum")
}

val PsTypography = Typography(bodyLarge = PsType.bodyLg, bodyMedium = PsType.body, labelLarge = PsType.button)
