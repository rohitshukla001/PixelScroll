package dev.rohitshukla.pixelscroll.ui.components

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.vector.PathParser
import androidx.compose.ui.unit.dp

/** The design system's 24 dp line icons: round caps and joins, drawn in the tint colour. */
object PsIcons {
    val Back = line("M19 12H5M11 18l-6-6 6-6")
    val Check = line("M5 12.5l4.5 4.5L19 7")
    val Ban = line("$CIRCLE_9 M5.6 5.6l12.8 12.8")
    val CheckCircle = line("$CIRCLE_9 M8.5 12.5l2.5 2.5 4.5-5")
    val AlertCircle = line("$CIRCLE_9 M12 7.5v5.5M12 16.5v.01")

    /** The Pixel Scroll mark: two upward chevrons, "next, next"; the lower one fainter. */
    val Mark = ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addStroke(MARK_TOP, width = 2.75f)
        .addStroke(MARK_BOTTOM, width = 2.75f, alpha = 0.45f)
        .build()
}

private const val CIRCLE_9 = "M3 12a9 9 0 1 0 18 0a9 9 0 1 0 -18 0"
private const val MARK_TOP = "M4.5 11.5L12 4l7.5 7.5"
private const val MARK_BOTTOM = "M4.5 20L12 12.5l7.5 7.5"

private fun line(path: String) =
    ImageVector.Builder(defaultWidth = 24.dp, defaultHeight = 24.dp, viewportWidth = 24f, viewportHeight = 24f)
        .addStroke(path, width = 1.75f)
        .build()

private fun ImageVector.Builder.addStroke(path: String, width: Float, alpha: Float = 1f) = addPath(
    pathData = PathParser().parsePathString(path).toNodes(),
    stroke = SolidColor(Color.Black),
    strokeAlpha = alpha,
    strokeLineWidth = width,
    strokeLineCap = StrokeCap.Round,
    strokeLineJoin = StrokeJoin.Round,
)
