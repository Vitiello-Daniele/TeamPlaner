package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.painterResource
import de.teamplaner.R
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRole

private enum class AppTab(val title: String) {
    Profile("Profil"),
    Team("Team"),
    Events("Termine")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainAppScreen(
    name: String,
    onLogoutClick: () -> Unit
) {
    val displayName = name.ifBlank { "Kein Name angegeben" }
    var selectedTab by remember { mutableStateOf(AppTab.Profile) }
    var team by remember { mutableStateOf<Team?>(null) }
    var events by remember { mutableStateOf(emptyList<TeamEvent>()) }

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
            AppTab.Profile -> ProfileScreen(
                name = displayName,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.Team -> TeamScreen(
                team = team,
                onTeamCreate = { teamName ->
                    team = Team(
                        name = teamName,
                        inviteCode = createInviteCode(teamName),
                        members = listOf(
                            TeamMember(
                                name = displayName,
                                role = TeamRole.Trainer
                            )
                        )
                    )
                },
                onTeamJoin = { inviteCode ->
                    team = Team(
                        name = "Team $inviteCode",
                        inviteCode = inviteCode,
                        members = listOf(
                            TeamMember(
                                name = displayName,
                                role = TeamRole.Member
                            )
                        )
                    )
                },
                onMemberAdd = { memberName ->
                    val currentTeam = team
                    val trimmedName = memberName.trim()

                    if (currentTeam != null && trimmedName.isNotBlank()) {
                        team = currentTeam.copy(
                            members = currentTeam.members + TeamMember(
                                name = trimmedName,
                                role = TeamRole.Member
                            )
                        )
                    }
                },
                onMemberRemove = { member ->
                    val currentTeam = team

                    if (currentTeam != null && member.role != TeamRole.Trainer) {
                        team = currentTeam.copy(
                            members = currentTeam.members - member
                        )
                    }
                },
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.Events -> EventScreen(
                team = team,
                events = events,
                onEventCreate = { event ->
                    events = events + event
                },
                onEventRemove = { event ->
                    events = events - event
                },
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

private fun createInviteCode(teamName: String): String {
    val codeBase = teamName
        .filter { it.isLetterOrDigit() }
        .uppercase()
        .take(4)

    return codeBase.ifBlank { "TEAM" } + "01"
}
