package de.teamplaner.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun ProfileScreen(
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
            modifier = Modifier.fieldTopPadding(12),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
