package de.teamplaner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
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
import androidx.compose.ui.unit.dp
import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRole

private sealed interface TeamView {
    data object List : TeamView
    data object Create : TeamView
    data object Join : TeamView
    data object Detail : TeamView
}

private enum class TeamDetailTab(val title: String) {
    Overview("Übersicht"),
    Members("Mitglieder"),
    Events("Termine"),
    Duties("Dienste"),
    Plan("Plan")
}

@Composable
fun TeamScreen(
    teams: List<Team>,
    selectedTeam: Team?,
    currentMember: TeamMember?,
    canManageSelectedTeam: Boolean,
    selectedTeamEvents: List<TeamEvent>,
    selectedTeamDuties: List<Duty>,
    selectedTeamAssignments: List<DutyAssignment>,
    onTeamSelect: (String) -> Unit,
    onTeamCreate: (String) -> Unit,
    onTeamJoin: (String) -> Unit,
    onMemberAdd: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    onEventCreate: (TeamEvent) -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
    onDutyCreate: (Duty) -> Unit,
    onDutyRemove: (Duty) -> Unit,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit,
    onFairPlanCreate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var teamView by remember { mutableStateOf<TeamView>(TeamView.List) }

    when (teamView) {
        TeamView.List -> TeamListContent(
            teams = teams,
            onCreateClick = { teamView = TeamView.Create },
            onJoinClick = { teamView = TeamView.Join },
            onTeamClick = { team ->
                onTeamSelect(team.id)
                teamView = TeamView.Detail
            },
            modifier = modifier
        )
        TeamView.Create -> CreateTeamContent(
            onSaveClick = { name ->
                onTeamCreate(name)
                teamView = TeamView.Detail
            },
            onBackClick = { teamView = TeamView.List },
            modifier = modifier
        )
        TeamView.Join -> JoinTeamContent(
            onJoinClick = { inviteCode ->
                onTeamJoin(inviteCode)
                teamView = TeamView.Detail
            },
            onBackClick = { teamView = TeamView.List },
            modifier = modifier
        )
        TeamView.Detail -> {
            if (selectedTeam == null) {
                teamView = TeamView.List
            } else {
                TeamDetailContent(
                    team = selectedTeam,
                    currentMember = currentMember,
                    canManageTeam = canManageSelectedTeam,
                    events = selectedTeamEvents,
                    duties = selectedTeamDuties,
                    assignments = selectedTeamAssignments,
                    onBackClick = { teamView = TeamView.List },
                    onMemberAdd = onMemberAdd,
                    onMemberRemove = onMemberRemove,
                    onInviteCodeRefresh = onInviteCodeRefresh,
                    onInviteCodeDeactivate = onInviteCodeDeactivate,
                    onEventCreate = onEventCreate,
                    onEventUpdate = onEventUpdate,
                    onEventRemove = onEventRemove,
                    onDutyCreate = onDutyCreate,
                    onDutyRemove = onDutyRemove,
                    onDutyAssign = onDutyAssign,
                    onAssignmentRemove = onAssignmentRemove,
                    onFairPlanCreate = onFairPlanCreate,
                    modifier = modifier
                )
            }
        }
    }
}

@Composable
private fun TeamListContent(
    teams: List<Team>,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onTeamClick: (Team) -> Unit,
    modifier: Modifier = Modifier
) {
    var query by remember { mutableStateOf("") }
    val filteredTeams = teams.filter { it.name.contains(query.trim(), ignoreCase = true) }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Teams", style = MaterialTheme.typography.headlineMedium)
        AuthTextField(
            value = query,
            onValueChange = { query = it },
            label = "Team suchen",
            modifier = Modifier.fieldTopPadding(24)
        )
        Row(modifier = Modifier.fieldTopPadding(16)) {
            Button(onClick = onCreateClick) {
                Text(text = "+")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onJoinClick) {
                Text(text = "Invite-Code")
            }
        }
        if (teams.isEmpty()) {
            Text(
                text = "Du bist noch in keinem Team",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            filteredTeams.forEach { team ->
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                        .clickable { onTeamClick(team) }
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = team.name, style = MaterialTheme.typography.titleMedium)
                        Text(
                            text = "${team.members.size} Mitglieder",
                            modifier = Modifier.fieldTopPadding(4),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TeamDetailContent(
    team: Team,
    currentMember: TeamMember?,
    canManageTeam: Boolean,
    events: List<TeamEvent>,
    duties: List<Duty>,
    assignments: List<DutyAssignment>,
    onBackClick: () -> Unit,
    onMemberAdd: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    onEventCreate: (TeamEvent) -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
    onDutyCreate: (Duty) -> Unit,
    onDutyRemove: (Duty) -> Unit,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit,
    onFairPlanCreate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var tab by remember(team.id) { mutableStateOf(TeamDetailTab.Overview) }

    when (tab) {
        TeamDetailTab.Events -> Column(modifier = modifier.fillMaxSize()) {
            TeamToolHeader(team = team, selectedTab = tab, onBackClick = onBackClick, onTabSelect = { tab = it })
            EventScreen(
                team = team,
                currentMember = currentMember,
                canManageEvents = canManageTeam,
                events = events,
                duties = duties,
                onEventCreate = onEventCreate,
                onEventUpdate = onEventUpdate,
                onEventRemove = onEventRemove,
                onAutoAssign = { onFairPlanCreate(false) },
                modifier = Modifier.weight(1f)
            )
        }
        TeamDetailTab.Duties -> Column(modifier = modifier.fillMaxSize()) {
            TeamToolHeader(team = team, selectedTab = tab, onBackClick = onBackClick, onTabSelect = { tab = it })
            DutyScreen(
                team = team,
                canManageDuties = canManageTeam,
                duties = duties,
                onDutyCreate = onDutyCreate,
                onDutyRemove = onDutyRemove,
                modifier = Modifier.weight(1f)
            )
        }
        TeamDetailTab.Plan -> Column(modifier = modifier.fillMaxSize()) {
            TeamToolHeader(team = team, selectedTab = tab, onBackClick = onBackClick, onTabSelect = { tab = it })
            PlanScreen(
                team = team,
                currentMember = currentMember,
                events = events,
                duties = duties,
                assignments = assignments,
                canManageAssignments = canManageTeam,
                onDutyAssign = onDutyAssign,
                onAssignmentRemove = onAssignmentRemove,
                onFairPlanCreate = onFairPlanCreate,
                modifier = Modifier.weight(1f)
            )
        }
        else -> TeamDetailOverview(
            team = team,
            tab = tab,
            canManageTeam = canManageTeam,
            onBackClick = onBackClick,
            onTabSelect = { tab = it },
            onMemberAdd = onMemberAdd,
            onMemberRemove = onMemberRemove,
            onInviteCodeRefresh = onInviteCodeRefresh,
            onInviteCodeDeactivate = onInviteCodeDeactivate,
            modifier = modifier
        )
    }
}

@Composable
private fun TeamToolHeader(
    team: Team,
    selectedTab: TeamDetailTab,
    onBackClick: () -> Unit,
    onTabSelect: (TeamDetailTab) -> Unit
) {
    Column(modifier = Modifier.padding(start = 16.dp, top = 16.dp, end = 16.dp)) {
        ScreenHeader(title = team.name, onBackClick = onBackClick)
        TeamDetailTabs(selectedTab = selectedTab, onTabSelect = onTabSelect)
    }
}

@Composable
private fun TeamDetailOverview(
    team: Team,
    tab: TeamDetailTab,
    canManageTeam: Boolean,
    onBackClick: () -> Unit,
    onTabSelect: (TeamDetailTab) -> Unit,
    onMemberAdd: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    modifier: Modifier = Modifier
) {
    var memberName by remember { mutableStateOf("") }
    var memberError by remember { mutableStateOf("") }
    val memberSuggestions = listOf("Leon M.", "Daniel", "David", "Dario", "Max", "Mia", "Laura", "Lea", "Tom", "Sara")

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(title = team.name, onBackClick = onBackClick)
        TeamDetailTabs(selectedTab = tab, onTabSelect = onTabSelect)

        if (tab == TeamDetailTab.Overview) {
            Text(
                text = if (team.inviteCodeActive) "Invite-Code: ${team.inviteCode}" else "Invite-Code ist deaktiviert",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
            if (canManageTeam) {
                Button(onClick = onInviteCodeRefresh, modifier = defaultActionModifier(topPadding = 12)) {
                    Text(text = "Neuen Invite-Code erstellen")
                }
                OutlinedButton(onClick = onInviteCodeDeactivate, modifier = defaultActionModifier(topPadding = 12)) {
                    Text(text = "Invite-Code deaktivieren")
                }
            }
        }

        if (tab == TeamDetailTab.Members) {
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
                MemberSuggestions(
                    query = memberName,
                    existingMembers = team.members,
                    suggestions = memberSuggestions,
                    onSuggestionClick = { memberName = it }
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
                    Text(text = "Mitglied hinzufügen")
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
private fun TeamDetailTabs(
    selectedTab: TeamDetailTab,
    onTabSelect: (TeamDetailTab) -> Unit
) {
    Row(modifier = Modifier.fieldTopPadding(16)) {
        TeamDetailTab.entries.forEach { tab ->
            FilterChip(
                selected = selectedTab == tab,
                onClick = { onTabSelect(tab) },
                label = { Text(tab.title) },
                modifier = Modifier.padding(end = 6.dp)
            )
        }
    }
}

@Composable
private fun MemberSuggestions(
    query: String,
    existingMembers: List<TeamMember>,
    suggestions: List<String>,
    onSuggestionClick: (String) -> Unit
) {
    val trimmedQuery = query.trim()

    if (trimmedQuery.length < 2) {
        return
    }

    val filteredSuggestions = suggestions
        .filter { suggestion ->
            suggestion.contains(trimmedQuery, ignoreCase = true) &&
                existingMembers.none { it.name.equals(suggestion, ignoreCase = true) }
        }
        .take(4)

    if (filteredSuggestions.isEmpty()) {
        return
    }

    Text(
        text = "Vorschläge",
        modifier = Modifier.fieldTopPadding(12),
        style = MaterialTheme.typography.titleSmall
    )
    filteredSuggestions.forEach { suggestion ->
        OutlinedButton(
            onClick = { onSuggestionClick(suggestion) },
            modifier = defaultActionModifier(topPadding = 8)
        ) {
            Text(text = suggestion)
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
        Text(text = "Team erstellen", style = MaterialTheme.typography.headlineMedium)
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
            Text(text = errorText, modifier = Modifier.fieldTopPadding(8), style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onBackClick, modifier = defaultActionModifier(topPadding = 12)) {
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
        Text(text = "Team beitreten", style = MaterialTheme.typography.headlineMedium)
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
            Text(text = errorText, modifier = Modifier.fieldTopPadding(8), style = MaterialTheme.typography.bodyMedium)
        }
        OutlinedButton(onClick = onBackClick, modifier = defaultActionModifier(topPadding = 12)) {
            Text(text = "Zurück")
        }
    }
}
