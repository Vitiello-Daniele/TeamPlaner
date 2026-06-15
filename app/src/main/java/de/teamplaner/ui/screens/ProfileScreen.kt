package de.teamplaner.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.teamplaner.model.Team
import de.teamplaner.model.TeamRole

@Composable
fun ProfileScreen(
    name: String,
    teams: List<Team>,
    modifier: Modifier = Modifier
) {
    val trainerCount = teams.count { team ->
        team.members.any { it.name == name && it.role == TeamRole.Trainer }
    }

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
        Text(
            text = "${teams.size} Teams",
            modifier = Modifier.fieldTopPadding(12),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "$trainerCount Trainer-Teams",
            modifier = Modifier.fieldTopPadding(12),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
