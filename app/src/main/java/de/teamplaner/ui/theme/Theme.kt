package de.teamplaner.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable

private val LightColors = lightColorScheme(
    primary = TeamBlue,
    secondary = TeamGreen,
    onBackground = TeamDark
)

@Composable
fun TeamPlanerTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = LightColors,
        typography = TeamTypography,
        content = content
    )
}
