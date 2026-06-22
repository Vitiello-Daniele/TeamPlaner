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
    onRegistrationClick: (String, String, String, String, (String) -> Unit) -> Unit,
    onBackClick: () -> Unit
) {
    var firstName by remember { mutableStateOf("") }
    var lastName by remember { mutableStateOf("") }
    var email by remember { mutableStateOf("") }
    var password by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

    ScreenContent {
        Text(
            text = "Registrieren",
            style = MaterialTheme.typography.headlineMedium
        )
        AuthTextField(
            value = firstName,
            onValueChange = { firstName = it },
            label = "Vorname",
            modifier = Modifier.fieldTopPadding(32)
        )
        AuthTextField(
            value = lastName,
            onValueChange = { lastName = it },
            label = "Nachname",
            modifier = Modifier.fieldTopPadding(12)
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
        if (errorText.isNotBlank()) {
            ErrorMessage(
                text = errorText,
                modifier = Modifier.fieldTopPadding(12)
            )
        }
        Button(
            onClick = {
                errorText = ""
                val trimmedFirstName = firstName.trim()
                val trimmedLastName = lastName.trim()
                val trimmedEmail = email.trim()
                val namePattern = Regex("^[A-Za-zÄÖÜäöüß'-]+$")
                val emailPattern = Regex("^[^\\s@]+@[^\\s@]+\\.[^\\s@]+$")

                when {
                    trimmedFirstName.isBlank() -> errorText = "Bitte einen Vornamen eingeben"
                    trimmedLastName.isBlank() -> errorText = "Bitte einen Nachnamen eingeben"
                    trimmedEmail.isBlank() -> errorText = "Bitte eine E-Mail eingeben"
                    password.isBlank() -> errorText = "Bitte ein Passwort eingeben"
                    !namePattern.matches(trimmedFirstName) -> errorText = "Vorname enthält ungültige Zeichen"
                    !namePattern.matches(trimmedLastName) -> errorText = "Nachname enthält ungültige Zeichen"
                    !emailPattern.matches(trimmedEmail) -> errorText = "Bitte eine gültige E-Mail eingeben"
                    password.length < 6 -> errorText = "Das Passwort braucht mindestens 6 Zeichen"
                    else -> onRegistrationClick(
                        trimmedFirstName,
                        trimmedLastName,
                        trimmedEmail,
                        password
                    ) { errorText = it }
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Registrieren")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = defaultActionModifier(topPadding = 12)
        ) {
            Text(text = "Zurück")
        }
    }
}
