package de.teamplaner.ui.screens

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import de.teamplaner.model.Team
import de.teamplaner.model.TeamMember

@Composable
fun ProfileScreen(
    name: String,
    team: Team?,
    currentMember: TeamMember?,
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
        Text(
            text = currentMember?.role?.label ?: "Noch keine Rolle",
            modifier = Modifier.fieldTopPadding(12),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = team?.name ?: "Noch kein Team",
            modifier = Modifier.fieldTopPadding(12),
            style = MaterialTheme.typography.bodyLarge
        )
    }
}
