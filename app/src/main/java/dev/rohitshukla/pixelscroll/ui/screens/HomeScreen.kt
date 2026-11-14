package dev.rohitshukla.pixelscroll.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.toggleable
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.rohitshukla.pixelscroll.R
import dev.rohitshukla.pixelscroll.data.Settings
import dev.rohitshukla.pixelscroll.ui.components.Card
import dev.rohitshukla.pixelscroll.ui.components.DelaySlider
import dev.rohitshukla.pixelscroll.ui.components.Divider
import dev.rohitshukla.pixelscroll.ui.components.Footer
import dev.rohitshukla.pixelscroll.ui.components.PsSnackbarHost
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import dev.rohitshukla.pixelscroll.ui.components.Logo
import dev.rohitshukla.pixelscroll.ui.components.LogoSize
import dev.rohitshukla.pixelscroll.ui.components.SmallSecondaryButton
import dev.rohitshukla.pixelscroll.ui.components.StatusRow
import dev.rohitshukla.pixelscroll.ui.components.SwitchVisual
import dev.rohitshukla.pixelscroll.ui.theme.Ps
import dev.rohitshukla.pixelscroll.ui.theme.PsType
import dev.rohitshukla.pixelscroll.ui.theme.Radius
import dev.rohitshukla.pixelscroll.ui.theme.Space

/** How long the delay confirmation stays on screen. */
private const val SNACKBAR_MS = 3_000L

/** Screen 3, the only main screen: the power card, the service status and the fallback delay. */
@Composable
fun HomeScreen(
    settings: Settings,
    serviceConnected: Boolean,
    onToggle: (Boolean) -> Unit,
    onDelayChange: (Int) -> Unit,
    onFix: () -> Unit,
) {
    val snackbar = remember { SnackbarHostState() }
    val scope = rememberCoroutineScope()
    val context = LocalContext.current
    var snackbarJob by remember { mutableStateOf<Job?>(null) }
    // Confirms each new delay for SNACKBAR_MS; a newer one replaces the one on screen.
    val onDelaySet: (Int) -> Unit = { seconds ->
        onDelayChange(seconds)
        snackbarJob?.cancel()
        snackbar.currentSnackbarData?.dismiss()
        snackbarJob = scope.launch {
            // Material's shortest duration is 4 s: show it until dismissed, and dismiss after 3 s.
            launch {
                delay(SNACKBAR_MS)
                snackbar.currentSnackbarData?.dismiss()
            }
            snackbar.showSnackbar(context.getString(R.string.delay_set, seconds), duration = SnackbarDuration.Indefinite)
        }
    }
    Box(Modifier.fillMaxSize()) {
        Column(Modifier.fillMaxSize().systemBarsPadding()) {
            Row(
                Modifier
                    .fillMaxWidth()
                    .height(64.dp)
                    .padding(horizontal = Space.s5),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                Logo(LogoSize.Small, contentDescription = null)
                Text(stringResource(R.string.app_name), style = PsType.titleStrong, color = Ps.colors.textPrimary)
            }
            Column(
                Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(start = Space.s5, end = Space.s5, top = Space.s2, bottom = Space.s5),
                verticalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                PowerCard(enabled = settings.enabled, serviceConnected = serviceConnected, onToggle = onToggle)
                if (serviceConnected) {
                    StatusRow(connected = true, title = stringResource(R.string.service_connected), detail = null, action = null)
                } else {
                    StatusRow(
                        connected = false,
                        title = stringResource(R.string.service_off),
                        detail = stringResource(R.string.service_off_detail),
                        action = { SmallSecondaryButton(stringResource(R.string.fix), onClick = onFix) },
                    )
                }
                FallbackDelayCard(settings.fallbackDelaySec, onDelaySet)
            }
            Footer()
        }
        PsSnackbarHost(
            snackbar,
            Modifier
                .align(Alignment.BottomCenter)
                .systemBarsPadding()
                .padding(start = Space.s5, end = Space.s5, bottom = 56.dp),
        )
    }
}

@Composable
private fun PowerCard(enabled: Boolean, serviceConnected: Boolean, onToggle: (Boolean) -> Unit) {
    val c = Ps.colors
    val on = enabled && serviceConnected
    Card(
        borderColor = if (on) c.primary else c.outline,
        padding = PaddingValues(Space.s6),
        modifier = Modifier.clip(RoundedCornerShape(Radius.lg)).toggleable(
            value = on,
            enabled = serviceConnected,
            role = Role.Switch,
            onValueChange = onToggle,
        ),
    ) {
        Row(
            Modifier.fillMaxWidth().heightIn(min = 48.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(stringResource(R.string.auto_scroll_eyebrow), style = PsType.overline, color = c.textSecondary)
            SwitchVisual(checked = on, enabled = serviceConnected)
        }
        Text(
            stringResource(if (on) R.string.auto_scroll_on else R.string.auto_scroll_off),
            style = PsType.displaySm,
            color = if (serviceConnected) c.textPrimary else c.textSecondary,
        )
        Divider()
        Text(
            stringResource(
                when {
                    !serviceConnected -> R.string.hint_no_service
                    on -> R.string.hint_on
                    else -> R.string.hint_off
                },
            ),
            style = PsType.body,
            color = c.textSecondary,
        )
    }
}

@Composable
private fun FallbackDelayCard(savedSeconds: Int, onDelayChange: (Int) -> Unit) {
    val c = Ps.colors
    // Follows the finger while dragging; saved once when the drag ends.
    var seconds by remember { mutableIntStateOf(savedSeconds) }
    LaunchedEffect(savedSeconds) { seconds = savedSeconds }
    Card {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween,
        ) {
            Text(
                stringResource(R.string.fallback_delay),
                style = PsType.title,
                color = c.textPrimary,
                modifier = Modifier.semantics { heading() },
            )
            Text(stringResource(R.string.seconds, seconds), style = PsType.value, color = c.textPrimary)
        }
        Column {
            Text(stringResource(R.string.fallback_delay_hint), style = PsType.body, color = c.textSecondary)
            Spacer(Modifier.height(Space.s1))
            DelaySlider(
                value = seconds,
                range = Settings.MIN_DELAY_SEC..Settings.MAX_DELAY_SEC,
                onValueChange = { seconds = it },
                onValueChangeFinished = { onDelayChange(seconds) },
                contentDescription = stringResource(R.string.fallback_delay_a11y),
                minLabel = stringResource(R.string.seconds, Settings.MIN_DELAY_SEC),
                maxLabel = stringResource(R.string.seconds, Settings.MAX_DELAY_SEC),
            )
        }
    }
}
