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
import de.teamplaner.model.Team

private enum class TeamView {
    Overview,
    Create,
    Join
}

@Composable
fun TeamScreen(
    team: Team?,
    onTeamCreate: (String) -> Unit,
    onTeamJoin: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var teamView by remember { mutableStateOf(TeamView.Overview) }

    when (teamView) {
        TeamView.Overview -> TeamOverviewContent(
            team = team,
            onCreateClick = { teamView = TeamView.Create },
            onJoinClick = { teamView = TeamView.Join },
            modifier = modifier
        )
        TeamView.Create -> CreateTeamContent(
            onSaveClick = { name ->
                onTeamCreate(name)
                teamView = TeamView.Overview
            },
            onBackClick = { teamView = TeamView.Overview },
            modifier = modifier
        )
        TeamView.Join -> JoinTeamContent(
            onJoinClick = { inviteCode ->
                onTeamJoin(inviteCode)
                teamView = TeamView.Overview
            },
            onBackClick = { teamView = TeamView.Overview },
            modifier = modifier
        )
    }
}

@Composable
private fun TeamOverviewContent(
    team: Team?,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    ScreenContent(modifier = modifier) {
        Text(
            text = "Team",
            style = MaterialTheme.typography.headlineMedium
        )
        if (team == null) {
            Text(
                text = "Du bist noch in keinem Team",
                modifier = Modifier.fieldTopPadding(12),
                style = MaterialTheme.typography.bodyLarge
            )
            Button(
                onClick = onCreateClick,
                modifier = defaultActionModifier(topPadding = 32)
            ) {
                Text(text = "Team erstellen")
            }
            OutlinedButton(
                onClick = onJoinClick,
                modifier = defaultActionModifier(topPadding = 12)
            ) {
                Text(text = "Team beitreten")
            }
        } else {
            Text(
                text = team.name,
                modifier = Modifier.fieldTopPadding(12),
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = "Invite-Code: ${team.inviteCode}",
                modifier = Modifier.fieldTopPadding(12),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun CreateTeamContent(
    onSaveClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var teamName by remember { mutableStateOf("") }

    ScreenContent(modifier = modifier) {
        Text(
            text = "Team erstellen",
            style = MaterialTheme.typography.headlineMedium
        )
        AuthTextField(
            value = teamName,
            onValueChange = { teamName = it },
            label = "Teamname",
            modifier = Modifier.fieldTopPadding(32)
        )
        Button(
            onClick = { onSaveClick(teamName) },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Speichern")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = defaultActionModifier(topPadding = 12)
        ) {
            Text(text = "Zurück")
        }
    }
}

@Composable
private fun JoinTeamContent(
    onJoinClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inviteCode by remember { mutableStateOf("") }

    ScreenContent(modifier = modifier) {
        Text(
            text = "Team beitreten",
            style = MaterialTheme.typography.headlineMedium
        )
        AuthTextField(
            value = inviteCode,
            onValueChange = { inviteCode = it },
            label = "Invite-Code",
            modifier = Modifier.fieldTopPadding(32)
        )
        Button(
            onClick = { onJoinClick(inviteCode) },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Beitreten")
        }
        OutlinedButton(
            onClick = onBackClick,
            modifier = defaultActionModifier(topPadding = 12)
        ) {
            Text(text = "Zurück")
        }
    }
}
