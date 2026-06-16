package de.teamplaner.ui.screens

import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType

@Composable
fun RegistrationScreen(
    onRegistrationClick: (String, String, String, (String) -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    var name by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    ScreenContent {
        Text(
            text = "Registrieren",
            style = MaterialTheme.typography.headlineMedium
        )
        AuthTextField(
            value = name,
            onValueChange = { name = it },
            label = "Name",
            modifier = Modifier.fieldTopPadding(32)
        )
        AuthTextField(
            value = email,
            onValueChange = { email = it },
            label = "E-Mail",
            modifier = Modifier.fieldTopPadding(12),
            keyboardType = KeyboardType.Email
        )
        AuthTextField(
            value = password,
            onValueChange = { password = it },
            label = "Passwort",
            modifier = Modifier.fieldTopPadding(12),
            isPassword = true
        )
        Button(
            onClick = {
                errorText = ""
                onRegistrationClick(name.trim(), email.trim(), password) { errorText = it }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Registrieren")
        }
        if (errorText.isNotBlank()) {
            Text(
                text = errorText,
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = defaultActionModifier(topPadding = 12)
        ) {
            Text(text = "Zurück")
        }
    }
}
