package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import de.teamplaner.model.Team
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRole
import androidx.compose.ui.unit.dp

private enum class TeamView {
    Overview,
    Create,
    Join
}

@Composable
fun TeamScreen(
    team: Team?,
    canManageTeam: Boolean,
    onTeamCreate: (String) -> Unit,
    onTeamJoin: (String) -> Unit,
    onMemberAdd: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var teamView by remember { mutableStateOf(TeamView.Overview) }

    when (teamView) {
        TeamView.Overview -> TeamOverviewContent(
            team = team,
            canManageTeam = canManageTeam,
            onCreateClick = { teamView = TeamView.Create },
            onJoinClick = { teamView = TeamView.Join },
            onMemberAdd = onMemberAdd,
            onMemberRemove = onMemberRemove,
            onInviteCodeRefresh = onInviteCodeRefresh,
            onInviteCodeDeactivate = onInviteCodeDeactivate,
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
    canManageTeam: Boolean,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onMemberAdd: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var memberName by remember { mutableStateOf("") }
    var memberError by remember { mutableStateOf("") }

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
                text = if (team.inviteCodeActive) {
                    "Invite-Code: ${team.inviteCode}"
                } else {
                    "Invite-Code ist deaktiviert"
                },
                modifier = Modifier.fieldTopPadding(12),
                style = MaterialTheme.typography.bodyLarge
            )
            if (canManageTeam) {
                Button(
                    onClick = onInviteCodeRefresh,
                    modifier = defaultActionModifier(topPadding = 12)
                ) {
                    Text(text = "Neuen Invite-Code erstellen")
                }
                OutlinedButton(
                    onClick = onInviteCodeDeactivate,
                    modifier = defaultActionModifier(topPadding = 12)
                ) {
                    Text(text = "Invite-Code deaktivieren")
                }
            }
            Text(
                text = "Mitglieder",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.titleMedium
            )
            team.members.forEach { member ->
                TeamMemberRow(
                    member = member,
                    canRemove = canManageTeam,
                    onRemoveClick = { onMemberRemove(member) }
                )
            }
            if (canManageTeam) {
                AuthTextField(
                    value = memberName,
                    onValueChange = { memberName = it },
                    label = "Mitgliedsname",
                    modifier = Modifier.fieldTopPadding(24)
                )
                Button(
                    onClick = {
                        val trimmedName = memberName.trim()

                        if (trimmedName.isBlank()) {
                            memberError = "Bitte einen Namen eingeben"
                        } else if (team.members.any { it.name == trimmedName }) {
                            memberError = "Dieses Mitglied ist schon im Team"
                        } else {
                            onMemberAdd(trimmedName)
                            memberName = ""
                            memberError = ""
                        }
                    },
                    modifier = defaultActionModifier(topPadding = 12)
                ) {
                    Text(text = "Mitglied hinzufuegen")
                }
                if (memberError.isNotBlank()) {
                    Text(
                        text = memberError,
                        modifier = Modifier.fieldTopPadding(8),
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
            }
        }
    }
}

@Composable
private fun TeamMemberRow(
    member: TeamMember,
    canRemove: Boolean,
    onRemoveClick: () -> Unit
) {
    Row(
        modifier = defaultActionModifier(topPadding = 8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = "${member.name} (${member.role.label})",
            modifier = Modifier.weight(1f),
            style = MaterialTheme.typography.bodyLarge
        )
        if (canRemove && member.role != TeamRole.Trainer) {
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onRemoveClick) {
                Text(text = "Entfernen")
            }
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
    var errorText by remember { mutableStateOf("") }

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
            onClick = {
                val trimmedName = teamName.trim()

                if (trimmedName.isBlank()) {
                    errorText = "Bitte einen Teamnamen eingeben"
                } else {
                    onSaveClick(trimmedName)
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Speichern")
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

@Composable
private fun JoinTeamContent(
    onJoinClick: (String) -> Unit,
    onBackClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    var inviteCode by remember { mutableStateOf("") }
    var errorText by remember { mutableStateOf("") }

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
            onClick = {
                val trimmedCode = inviteCode.trim()

                if (trimmedCode.isBlank()) {
                    errorText = "Bitte einen Invite-Code eingeben"
                } else {
                    onJoinClick(trimmedCode)
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Beitreten")
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
