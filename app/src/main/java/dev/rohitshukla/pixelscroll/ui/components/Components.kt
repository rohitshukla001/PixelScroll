package dev.rohitshukla.pixelscroll.ui.components

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.Slider
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size as GeoSize
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import dev.rohitshukla.pixelscroll.R
import dev.rohitshukla.pixelscroll.ui.theme.Ps
import dev.rohitshukla.pixelscroll.ui.theme.PsType
import dev.rohitshukla.pixelscroll.ui.theme.Radius
import dev.rohitshukla.pixelscroll.ui.theme.Size
import dev.rohitshukla.pixelscroll.ui.theme.Space

// Logo

enum class LogoSize(val tile: Dp, val radius: Dp, val mark: Dp) {
    Small(32.dp, 10.dp, 20.dp),
    Large(96.dp, Radius.xl, 56.dp),
}

/** The Pixel Scroll mark on a primary tile. It inverts in dark theme. */
@Composable
fun Logo(size: LogoSize, contentDescription: String?) {
    Box(
        Modifier
            .size(size.tile)
            .background(Ps.colors.primary, RoundedCornerShape(size.radius))
            .then(if (contentDescription != null) Modifier.semantics { this.contentDescription = contentDescription } else Modifier),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            PsIcons.Mark,
            contentDescription = null,
            tint = Ps.colors.onPrimary,
            modifier = Modifier.size(size.mark),
        )
    }
}

// Buttons

@Composable
fun PrimaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val shape = RoundedCornerShape(Radius.md)
    Box(
        modifier
            .fillMaxWidth()
            .height(Size.button)
            .clip(shape)
            .background(Ps.colors.primary)
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PsType.button, color = Ps.colors.onPrimary)
    }
}

@Composable
fun TertiaryButton(text: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(Size.tap)
            .clip(RoundedCornerShape(Radius.md))
            .clickable(role = Role.Button, onClick = onClick),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PsType.button, color = Ps.colors.textSecondary)
    }
}

/** The small secondary button: "Fix" in the status row. */
@Composable
fun SmallSecondaryButton(text: String, onClick: () -> Unit) {
    val shape = RoundedCornerShape(Radius.md)
    Box(
        Modifier
            .height(Size.tap)
            .clip(shape)
            .background(Ps.colors.surface)
            .border(1.dp, Ps.colors.outline, shape)
            .clickable(role = Role.Button, onClick = onClick)
            .padding(horizontal = Space.s5),
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PsType.label, color = Ps.colors.textPrimary)
    }
}

@Composable
fun IconButton(icon: ImageVector, contentDescription: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(Size.tap)
            .clip(RoundedCornerShape(Radius.md))
            .clickable(role = Role.Button, onClick = onClick)
            .semantics { this.contentDescription = contentDescription },
        contentAlignment = Alignment.Center,
    ) {
        Icon(icon, contentDescription = null, tint = Ps.colors.textPrimary)
    }
}

// Card and divider

@Composable
fun Card(
    modifier: Modifier = Modifier,
    borderColor: Color = Ps.colors.outline,
    padding: PaddingValues = PaddingValues(Space.s5),
    content: @Composable ColumnScope.() -> Unit,
) {
    val shape = RoundedCornerShape(Radius.lg)
    Column(
        modifier
            .fillMaxWidth()
            .clip(shape)
            .background(Ps.colors.surface)
            .border(1.dp, borderColor, shape)
            .padding(padding),
        verticalArrangement = Arrangement.spacedBy(Space.s3),
        content = content,
    )
}

@Composable
fun Divider(modifier: Modifier = Modifier) {
    Box(
        modifier
            .fillMaxWidth()
            .height(1.dp)
            .background(Ps.colors.divider),
    )
}

// Switch

enum class SwitchSize(val width: Dp, val height: Dp, val offThumb: Dp, val offInset: Dp, val onThumb: Dp, val onInset: Dp) {
    Medium(52.dp, 32.dp, 18.dp, 6.dp, 24.dp, 3.dp),
    Large(72.dp, 44.dp, 24.dp, 8.dp, 34.dp, 4.dp),
}

/**
 * The switch's look only. The caller makes the row around it toggleable, so the tap target and
 * the "on/off" announcement cover the whole control.
 */
@Composable
fun SwitchVisual(checked: Boolean, enabled: Boolean = true, size: SwitchSize = SwitchSize.Large) {
    val c = Ps.colors
    val track by animateColorAsState(if (checked && enabled) c.primary else c.surfaceContainer, label = "track")
    val border by animateColorAsState(
        when {
            !enabled -> c.disabled
            checked -> c.primary
            else -> c.outline
        },
        label = "border",
    )
    val thumbColor by animateColorAsState(
        when {
            !enabled -> c.disabled
            checked -> c.onPrimary
            else -> c.secondary
        },
        label = "thumb",
    )
    val on = checked && enabled
    val thumb by animateDpAsState(if (on) size.onThumb else size.offThumb, label = "thumbSize")
    val x by animateDpAsState(if (on) size.width - 2.dp - size.onInset - size.onThumb else size.offInset, label = "thumbX")
    Box(
        Modifier
            .size(size.width, size.height)
            .clip(CircleShape)
            .background(track)
            .border(1.dp, border, CircleShape),
        contentAlignment = Alignment.CenterStart,
    ) {
        Box(
            Modifier
                .offset { IntOffset(x.roundToPx(), 0) }
                .size(thumb)
                .background(thumbColor, CircleShape),
        )
    }
}

// Status row

/**
 * Shows whether the Accessibility service is connected: a solid green or red fill with white
 * icon and words (5.0:1 and 6.5:1), never colour alone.
 */
@Composable
fun StatusRow(connected: Boolean, title: String, detail: String?, action: (@Composable () -> Unit)?) {
    val c = Ps.colors
    val color = if (connected) c.success else c.error
    val shape = RoundedCornerShape(Radius.md)
    Row(
        Modifier
            .fillMaxWidth()
            .heightIn(min = Size.button)
            .clip(shape)
            .background(color)
            .padding(start = Space.s4, top = Space.s3, end = Space.s3, bottom = Space.s3),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(Space.s3),
    ) {
        Icon(
            if (connected) PsIcons.CheckCircle else PsIcons.AlertCircle,
            contentDescription = null,
            tint = Color.White,
            modifier = Modifier.size(20.dp),
        )
        Column(Modifier.weight(1f)) {
            Text(title, style = PsType.label, color = Color.White)
            if (detail != null) Text(detail, style = PsType.caption, color = Color.White)
        }
        action?.invoke()
    }
}

// Slider

/** Whole-second slider: primary active track and thumb, outline inactive track, 48 dp touch height. */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DelaySlider(
    value: Int,
    range: IntRange,
    onValueChange: (Int) -> Unit,
    onValueChangeFinished: () -> Unit,
    contentDescription: String,
    minLabel: String,
    maxLabel: String,
) {
    val c = Ps.colors
    Column {
        Slider(
            value = value.toFloat(),
            onValueChange = { onValueChange(it.toInt()) },
            onValueChangeFinished = onValueChangeFinished,
            valueRange = range.first.toFloat()..range.last.toFloat(),
            steps = range.last - range.first - 1,
            modifier = Modifier
                .padding(top = Space.s1)
                .height(Size.tap)
                .semantics { this.contentDescription = contentDescription },
            thumb = {
                Box(
                    Modifier
                        .size(32.dp)
                        .background(c.surface, CircleShape)
                        .padding(4.dp)
                        .background(c.primary, CircleShape),
                )
            },
            track = { state ->
                val fraction = (state.value - state.valueRange.start) / (state.valueRange.endInclusive - state.valueRange.start)
                Canvas(
                    Modifier
                        .fillMaxWidth()
                        .height(4.dp),
                ) {
                    val r = CornerRadius(size.height / 2)
                    drawRoundRect(c.outline, cornerRadius = r)
                    drawRoundRect(c.primary, size = GeoSize(size.width * fraction, size.height), topLeft = Offset.Zero, cornerRadius = r)
                }
            },
        )
        Row(Modifier.fillMaxWidth().offset(y = (-8).dp), horizontalArrangement = Arrangement.SpaceBetween) {
            Text(minLabel, style = PsType.caption.copy(fontFeatureSettings = "tnum"), color = c.textSecondary)
            Text(maxLabel, style = PsType.caption.copy(fontFeatureSettings = "tnum"), color = c.textSecondary)
        }
    }
}

// Footer

private const val AUTHOR_URL = "https://info.rohitshukla.net/"

/**
 * The credit line at the bottom of every screen. Tapping it opens the author's page; the whole
 * line is the tap target, 48 dp high, since the name alone is too small to tap.
 */
@Composable
fun Footer(modifier: Modifier = Modifier) {
    val c = Ps.colors
    val uriHandler = LocalUriHandler.current
    val lead = stringResource(R.string.credit_lead)
    val name = stringResource(R.string.credit_name)
    val text = buildAnnotatedString {
        append("$lead ")
        withStyle(SpanStyle(color = c.textPrimary, fontWeight = FontWeight.SemiBold)) { append(name) }
        append(".")
    }
    Box(
        modifier
            .fillMaxWidth()
            .padding(start = Space.s5, end = Space.s5, top = Space.s1, bottom = Space.s1)
            .heightIn(min = Size.tap)
            .clip(RoundedCornerShape(Radius.md))
            // No button role: it opens a web page. The click label tells screen readers where.
            .clickable(onClickLabel = stringResource(R.string.open_author_page)) {
                uriHandler.openUri(AUTHOR_URL)
            },
        contentAlignment = Alignment.Center,
    ) {
        Text(text, style = PsType.caption, color = c.textSecondary, textAlign = TextAlign.Center)
    }
}

// Snackbar

/** Short confirmations: primary fill, on-primary text, 12 dp corners, no shadow, no action. */
@Composable
fun PsSnackbarHost(state: SnackbarHostState, modifier: Modifier = Modifier) {
    SnackbarHost(state, modifier) { data ->
        Box(
            Modifier
                .fillMaxWidth()
                .heightIn(min = Size.tap)
                .background(Ps.colors.primary, RoundedCornerShape(Radius.md))
                .padding(horizontal = Space.s4, vertical = Space.s3),
            contentAlignment = Alignment.CenterStart,
        ) {
            Text(data.visuals.message, style = PsType.label, color = Ps.colors.onPrimary)
        }
    }
}
