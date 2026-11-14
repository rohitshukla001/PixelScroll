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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.heading
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import dev.rohitshukla.pixelscroll.R
import dev.rohitshukla.pixelscroll.ui.components.Card
import dev.rohitshukla.pixelscroll.ui.components.Divider
import dev.rohitshukla.pixelscroll.ui.components.Footer
import dev.rohitshukla.pixelscroll.ui.components.IconButton
import dev.rohitshukla.pixelscroll.ui.components.Logo
import dev.rohitshukla.pixelscroll.ui.components.LogoSize
import dev.rohitshukla.pixelscroll.ui.components.PrimaryButton
import dev.rohitshukla.pixelscroll.ui.components.PsIcons
import dev.rohitshukla.pixelscroll.ui.components.SwitchSize
import dev.rohitshukla.pixelscroll.ui.components.SwitchVisual
import dev.rohitshukla.pixelscroll.ui.components.TertiaryButton
import dev.rohitshukla.pixelscroll.ui.theme.Ps
import dev.rohitshukla.pixelscroll.ui.theme.PsType
import dev.rohitshukla.pixelscroll.ui.theme.Space

/** Screen 2, one-time setup: explains Accessibility access and opens Android's settings. */
@Composable
fun AccessScreen(onBack: () -> Unit, onOpenSettings: () -> Unit, onNotNow: () -> Unit) {
    val c = Ps.colors
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Box(Modifier.fillMaxWidth().height(56.dp).padding(horizontal = Space.s5), contentAlignment = Alignment.CenterStart) {
            IconButton(PsIcons.Back, stringResource(R.string.back), onBack, Modifier.offset(x = (-12).dp))
        }
        Column(
            Modifier
                .weight(1f)
                .verticalScroll(rememberScrollState())
                .padding(horizontal = Space.s5),
        ) {
            Text(
                stringResource(R.string.access_title),
                style = PsType.headline,
                color = c.textPrimary,
                modifier = Modifier.padding(top = Space.s2).semantics { heading() },
            )
            Text(
                stringResource(R.string.access_lead),
                style = PsType.bodyLg,
                color = c.textSecondary,
                modifier = Modifier.padding(top = Space.s2),
            )
            Spacer(Modifier.height(Space.s6))
            SettingPreview()
            Text(
                stringResource(R.string.access_retry),
                style = PsType.body,
                color = c.textSecondary,
                modifier = Modifier.padding(top = Space.s3),
            )
            ListSection(
                title = stringResource(R.string.what_it_does),
                icon = PsIcons.Check,
                tint = c.textPrimary,
                items = listOf(R.string.does_sees_app, R.string.does_reads_progress, R.string.does_swipes),
            )
            ListSection(
                title = stringResource(R.string.what_it_never_does),
                icon = PsIcons.Ban,
                tint = c.textSecondary,
                items = listOf(R.string.never_messages, R.string.never_personal_data, R.string.never_send),
            )
            Spacer(Modifier.height(Space.s6))
        }
        Column(
            Modifier.padding(horizontal = Space.s5),
            verticalArrangement = Arrangement.spacedBy(Space.s2),
        ) {
            PrimaryButton(stringResource(R.string.open_settings), onClick = onOpenSettings)
            TertiaryButton(stringResource(R.string.not_now), onClick = onNotNow)
        }
        Footer()
    }
}

/** A picture of the Android setting to turn on. Not interactive. */
@Composable
private fun SettingPreview() {
    val c = Ps.colors
    Card(padding = PaddingValues(Space.s4)) {
        Text(stringResource(R.string.access_path), style = PsType.caption, color = c.textSecondary)
        Column {
            Divider()
            Row(
                Modifier.padding(top = Space.s3),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                Logo(LogoSize.Small, contentDescription = null)
                Column(Modifier.weight(1f)) {
                    Text(stringResource(R.string.app_name), style = PsType.titleSm, color = c.textPrimary)
                    Text(stringResource(R.string.use_pixel_scroll), style = PsType.body, color = c.textSecondary)
                }
                SwitchVisual(checked = true, size = SwitchSize.Medium)
            }
        }
    }
}

@Composable
private fun ListSection(title: String, icon: ImageVector, tint: Color, items: List<Int>) {
    Column(Modifier.padding(top = Space.s6)) {
        Text(title, style = PsType.title, color = Ps.colors.textPrimary, modifier = Modifier.semantics { heading() })
        Spacer(Modifier.height(Space.s2))
        items.forEach { item ->
            Row(
                Modifier.heightIn(min = 36.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(Space.s3),
            ) {
                Icon(icon, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
                Text(stringResource(item), style = PsType.body, color = Ps.colors.textPrimary)
            }
        }
    }
}
