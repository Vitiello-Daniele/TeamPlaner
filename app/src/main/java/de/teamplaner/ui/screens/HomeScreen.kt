package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.teamplaner.R
import de.teamplaner.ui.theme.TeamPlanerTheme

private enum class HomeView {
    Start,
    Login,
    Registration,
    App
}

private enum class AppTab(val title: String) {
    Profile("Profil"),
    Team("Team")
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
            HomeView.Login -> LoginContent(
                onLoginClick = {
                    profileName = ""
                    currentView = HomeView.App
                },
                onBackClick = { currentView = HomeView.Start }
            )
            HomeView.Registration -> RegistrationContent(
                onRegistrationClick = { name ->
                    profileName = name
                    currentView = HomeView.App
                },
                onBackClick = { currentView = HomeView.Start }
            )
            HomeView.App -> LoggedInContent(
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
            modifier = Modifier
                .padding(top = 32.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Einloggen")
        }
        OutlinedButton(
            onClick = onRegistrationClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Registrieren")
        }
    }
}

@Composable
private fun LoginContent(
    onLoginClick: () -> Unit,
    onBackClick: () -> Unit
) {
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ScreenContent {
        Text(
            text = "Einloggen",
            style = MaterialTheme.typography.headlineMedium
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "E-Mail",
            modifier = Modifier.padding(top = 32.dp),
            keyboardType = KeyboardType.Email
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Passwort",
            modifier = Modifier.padding(top = 12.dp),
            isPassword = true
        )
        Button(
            onClick = onLoginClick,
            modifier = Modifier
                .padding(top = 24.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Einloggen")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Zurück")
        }
    }
}

@Composable
private fun RegistrationContent(
    onRegistrationClick: (String) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }

    ScreenContent {
        Text(
            text = "Registrieren",
            style = MaterialTheme.typography.headlineMedium
        )
        AuthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
            modifier = Modifier.padding(top = 32.dp)
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "E-Mail",
            modifier = Modifier.padding(top = 12.dp),
            keyboardType = KeyboardType.Email
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Passwort",
            modifier = Modifier.padding(top = 12.dp),
            isPassword = true
        )
        Button(
            onClick = { onRegistrationClick(name) },
            modifier = Modifier
                .padding(top = 24.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Registrieren")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = Modifier
                .padding(top = 12.dp)
                .widthIn(max = 320.dp)
                .fillMaxWidth()
        ) {
            Text(text = "Zurück")
        }
    }
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
private fun LoggedInContent(
    name: String,
    onLogoutClick: () -> Unit
) {
    val displayName = name.ifBlank { "Kein Name angegeben" }
    var selectedTab by remember { mutableStateOf(AppTab.Profile) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text(text = "Willkommen, $displayName") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = MaterialTheme.colorScheme.onPrimary,
                    actionIconContentColor = MaterialTheme.colorScheme.onPrimary
                ),
                actions = {
                    IconButton(onClick = onLogoutClick) {
                        Icon(
                            painter = painterResource(id = R.drawable.ic_logout),
                            contentDescription = "Abmelden"
                        )
                    }
                }
            )
        },
        bottomBar = {
            NavigationBar {
                AppTab.entries.forEach { tab ->
                    NavigationBarItem(
                        selected = selectedTab == tab,
                        onClick = { selectedTab = tab },
                        label = { Text(text = tab.title) },
                        icon = {}
                    )
                }
            }
        }
    ) { innerPadding ->
        when (selectedTab) {
            AppTab.Profile -> ProfileTabContent(
                name = displayName,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.Team -> TeamTabContent(
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun ProfileTabContent(
    name: String,
    modifier: Modifier = Modifier
) {
    ScreenContent(modifier = modifier) {
        Text(
            text = "Profil",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = name,
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun TeamTabContent(modifier: Modifier = Modifier) {
    ScreenContent(modifier = modifier) {
        Text(
            text = "Team",
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = "Noch kein Team vorhanden",
            modifier = Modifier.padding(top = 12.dp),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun AuthTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    keyboardType: KeyboardType = KeyboardType.Text,
    isPassword: Boolean = false
) {
    OutlinedTextField(
        value = value,
        onValueChange = onValueChange,
        label = { Text(text = label) },
        singleLine = true,
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType),
        visualTransformation = if (isPassword) {
            PasswordVisualTransformation()
        } else {
            VisualTransformation.None
        },
        modifier = modifier
            .widthIn(max = 320.dp)
            .fillMaxWidth()
    )
}

@Composable
private fun ScreenContent(
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .padding(24.dp),
        verticalArrangement = Arrangement.Center,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        content()
    }
}

@Preview(showBackground = true)
@Composable
private fun HomeScreenPreview() {
    TeamPlanerTheme {
        HomeScreen()
    }
}
