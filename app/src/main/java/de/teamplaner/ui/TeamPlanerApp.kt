package de.teamplaner.ui

import androidx.compose.runtime.Composable
import de.teamplaner.ui.screens.HomeScreen
import de.teamplaner.ui.theme.TeamPlanerTheme

@Composable
fun TeamPlanerApp() {
    TeamPlanerTheme {
        HomeScreen()
    }
}
