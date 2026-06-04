package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import de.teamplaner.ui.theme.TeamPlanerTheme

private enum class HomeView {
    Start,
    Login,
    Registration
}

@Composable
fun HomeScreen() {
    var currentView by remember { mutableStateOf(HomeView.Start) }

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
                onBackClick = { currentView = HomeView.Start }
            )
            HomeView.Registration -> RegistrationContent(
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
private fun LoginContent(onBackClick: () -> Unit) {
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
            onClick = {},
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
private fun RegistrationContent(onBackClick: () -> Unit) {
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
            onClick = {},
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
private fun ScreenContent(content: @Composable () -> Unit) {
    Column(
        modifier = Modifier.padding(24.dp),
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
