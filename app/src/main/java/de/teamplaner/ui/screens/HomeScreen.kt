package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import de.teamplaner.ui.theme.TeamPlanerTheme

private enum class HomeView {
    Start,
    Login,
    Registration,
    App
}

@Composable
fun HomeScreen() {
    var currentView by remember { mutableStateOf(HomeView.Start) }
    var profileName by remember { mutableStateOf("") }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentView) {
            HomeView.Start -> StartContent(
                onLoginClick = { currentView = HomeView.Login },
                onRegistrationClick = { currentView = HomeView.Registration }
            )
            HomeView.Login -> LoginScreen(
                onLoginClick = {
                    profileName = ""
                    currentView = HomeView.App
                },
                onBackClick = { currentView = HomeView.Start }
            )
            HomeView.Registration -> RegistrationScreen(
                onRegistrationClick = { name ->
                    profileName = name
                    currentView = HomeView.App
                },
                onBackClick = { currentView = HomeView.Start }
            )
            HomeView.App -> MainAppScreen(
                name = profileName,
                onLogoutClick = {
                    profileName = ""
                    currentView = HomeView.Start
                }
            )
        }
    }
}

@Composable
private fun StartContent(
    onLoginClick: () -> Unit,
    onRegistrationClick: () -> Unit
) {
    ScreenContent {
        Text(
            text = "TeamPlaner",
            style = MaterialTheme.typography.headlineLarge
        )
        Text(
            text = "Dienstplanung für Sportteams",
            style = MaterialTheme.typography.bodyLarge
        )
        Button(
            onClick = onLoginClick,
            modifier = defaultActionModifier(topPadding = 32)
        ) {
            Text(text = "Einloggen")
        }
        OutlinedButton(
            onClick = onRegistrationClick,
            modifier = defaultActionModifier(topPadding = 12)
        ) {
            Text(text = "Registrieren")
        }
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    TeamPlanerTheme {
        HomeScreen()
    }
}
