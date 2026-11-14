package dev.rohitshukla.pixelscroll.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import dev.rohitshukla.pixelscroll.R
import dev.rohitshukla.pixelscroll.ui.components.Footer
import dev.rohitshukla.pixelscroll.ui.components.Logo
import dev.rohitshukla.pixelscroll.ui.components.LogoSize
import dev.rohitshukla.pixelscroll.ui.components.PrimaryButton
import dev.rohitshukla.pixelscroll.ui.theme.Ps
import dev.rohitshukla.pixelscroll.ui.theme.PsType
import dev.rohitshukla.pixelscroll.ui.theme.Space

/** Screen 1, first launch only: the logo between two hairlines, the name, the tagline, "Get started". */
@Composable
fun WelcomeScreen(onGetStarted: () -> Unit) {
    val c = Ps.colors
    Column(Modifier.fillMaxSize().systemBarsPadding()) {
        Column(
            Modifier.weight(1f).fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Hairline()
            Row(Modifier.height(160.dp)) {
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.outline))
                Box(Modifier.size(158.dp, 160.dp).background(c.surface), contentAlignment = Alignment.Center) {
                    Logo(LogoSize.Large, contentDescription = stringResource(R.string.app_name))
                }
                Box(Modifier.width(1.dp).fillMaxHeight().background(c.outline))
            }
            Hairline()
            Spacer(Modifier.height(Space.s10))
            Text(stringResource(R.string.app_name), style = PsType.display, color = c.textPrimary)
            Spacer(Modifier.height(Space.s2))
            Text(
                stringResource(R.string.tagline),
                style = PsType.bodyLg,
                color = c.textSecondary,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(horizontal = Space.s5),
            )
        }
        PrimaryButton(
            stringResource(R.string.get_started),
            onClick = onGetStarted,
            modifier = Modifier.padding(horizontal = Space.s5),
        )
        Footer()
    }
}

@Composable
private fun Hairline() {
    Box(Modifier.fillMaxWidth().height(1.dp).background(Ps.colors.outline))
}
