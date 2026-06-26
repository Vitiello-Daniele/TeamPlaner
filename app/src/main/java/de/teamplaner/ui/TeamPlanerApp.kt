package de.teamplaner.ui

import androidx.compose.runtime.Composable
import de.teamplaner.ui.screens.HomeScreen
import de.teamplaner.ui.theme.TeamPlanerTheme

// Einstieg der Login-Activity (zeigt die Auth-Oberfläche).
@Composable
fun TeamPlanerApp(onAuthenticated: (token: String, name: String) -> Unit) {
    TeamPlanerTheme {
        HomeScreen(onAuthenticated = onAuthenticated)
    }
}
