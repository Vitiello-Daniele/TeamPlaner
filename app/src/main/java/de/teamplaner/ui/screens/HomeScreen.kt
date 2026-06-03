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
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import de.teamplaner.ui.theme.TeamPlanerTheme

@Composable
fun HomeScreen() {
    Surface(
        modifier = Modifier.fillMaxSize(),
        color = MaterialTheme.colorScheme.background
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "TeamPlaner",
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                text = "Dienstplanung für Sportteams",
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = {},
                modifier = Modifier
                    .padding(top = 32.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Einloggen")
            }
            OutlinedButton(
                onClick = {},
                modifier = Modifier
                    .padding(top = 12.dp)
                    .widthIn(max = 320.dp)
                    .fillMaxWidth()
            ) {
                Text(text = "Registrieren")
            }
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
