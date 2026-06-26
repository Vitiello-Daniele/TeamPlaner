package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import de.teamplaner.data.auth.AuthApiClient
import de.teamplaner.data.auth.AuthSession
import de.teamplaner.data.auth.AuthSessionStore
import de.teamplaner.ui.theme.TeamPlanerTheme
import kotlinx.coroutines.launch

private enum class HomeView {
    Loading,
    Start,
    Login,
    Registration
}

// Login-Oberfläche: Start, Login, Registrierung.
// Bei erfolgreicher Anmeldung: onAuthenticated wechselt zur AppActivity.
@Composable
fun HomeScreen(onAuthenticated: (token: String, name: String) -> Unit) {
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val sessionStore = remember(context) { AuthSessionStore(context) }
    val authApiClient = remember { AuthApiClient() }
    var currentView by remember { mutableStateOf(HomeView.Loading) }

    // Sitzung speichern, dann weiter zur App
    fun openApp(session: AuthSession) {
        sessionStore.save(session)
        onAuthenticated(session.token, session.user.name)
    }

    // gespeicherte Sitzung prüfen (Auto-Login)
    LaunchedEffect(Unit) {
        val savedSession = sessionStore.load()
        if (savedSession == null) {
            currentView = HomeView.Start
        } else {
            authApiClient.me(savedSession.token)
                .onSuccess { user ->
                    onAuthenticated(savedSession.token, user.name)
                }
                .onFailure {
                    sessionStore.clear()
                    currentView = HomeView.Start
                }
        }
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        when (currentView) {
            HomeView.Loading -> ScreenContent {
                Text(text = "Laden")
            }
            HomeView.Start -> StartContent(
                onLoginClick = { currentView = HomeView.Login },
                onRegistrationClick = { currentView = HomeView.Registration }
            )
            HomeView.Login -> LoginScreen(
                onLoginClick = { email, password, onError ->
                    scope.launch {
                        authApiClient.login(email, password)
                            .onSuccess(::openApp)
                            .onFailure { onError(it.message ?: "Login fehlgeschlagen") }
                    }
                },
                onDebugLoginClick = { email, password, onError ->
                    scope.launch {
                        authApiClient.login(email, password)
                            .onSuccess(::openApp)
                            .onFailure { onError(it.message ?: "Login fehlgeschlagen") }
                    }
                },
                onBackClick = { currentView = HomeView.Start }
            )
            HomeView.Registration -> RegistrationScreen(
                onRegistrationClick = { firstName, lastName, email, password, onError ->
                    scope.launch {
                        authApiClient.register(firstName, lastName, email, password)
                            .onSuccess(::openApp)
                            .onFailure { onError(it.message ?: "Registrierung fehlgeschlagen") }
                    }
                },
                onBackClick = { currentView = HomeView.Start }
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
        HomeScreen(onAuthenticated = { _, _ -> })
    }
}
