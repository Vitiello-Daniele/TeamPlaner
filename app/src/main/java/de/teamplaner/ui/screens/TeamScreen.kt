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
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.FilterChip
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.teamplaner.model.TeamRequest
import de.teamplaner.model.TeamRole
import de.teamplaner.model.UserSearchResult

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
    openInvites: List<TeamRequest>,
    openJoinRequests: List<TeamRequest>,
    selectedTeamJoinRequests: List<TeamRequest>,
    selectedTeamInvites: List<TeamRequest>,
    memberSuggestions: List<UserSearchResult>,
    teamName: (String) -> String,
    onTeamSelect: (String) -> Unit,
    onTeamCreate: (String) -> Unit,
    onTeamJoin: (String, (String?) -> Unit) -> Unit,
    onMemberSearch: (String) -> Unit,
    onMemberInvite: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onRequestAccept: (TeamRequest) -> Unit,
    onRequestReject: (TeamRequest) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    onTeamRemove: () -> Unit,
    onEventCreate: (TeamEvent, Boolean) -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
    onDutyCreate: (Duty) -> Unit,
    onDutyRemove: (Duty) -> Unit,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit,
    onFairPlanCreate: (TeamEvent, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var teamView by remember { mutableStateOf<TeamView>(TeamView.List) }

    when (teamView) {
        TeamView.List -> TeamListContent(
            teams = teams,
            openInvites = openInvites,
            openJoinRequests = openJoinRequests,
            teamName = teamName,
            onCreateClick = { teamView = TeamView.Create },
            onJoinClick = { teamView = TeamView.Join },
            onInviteAccept = onRequestAccept,
            onInviteReject = onRequestReject,
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
            onJoinClick = { inviteCode, onResult ->
                onTeamJoin(inviteCode) { errorText ->
                    if (errorText == null) {
                        onResult(null)
                        teamView = TeamView.List
                    } else {
                        onResult(errorText)
                    }
                }
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
                    joinRequests = selectedTeamJoinRequests,
                    invites = selectedTeamInvites,
                    memberSuggestions = memberSuggestions,
                    onBackClick = { teamView = TeamView.List },
                    onMemberSearch = onMemberSearch,
                    onMemberInvite = onMemberInvite,
                    onMemberRemove = onMemberRemove,
                    onRequestAccept = onRequestAccept,
                    onRequestReject = onRequestReject,
                    onInviteCodeRefresh = onInviteCodeRefresh,
                    onInviteCodeDeactivate = onInviteCodeDeactivate,
                    onTeamRemove = {
                        onTeamRemove()
                        teamView = TeamView.List
                    },
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
    openInvites: List<TeamRequest>,
    openJoinRequests: List<TeamRequest>,
    teamName: (String) -> String,
    onCreateClick: () -> Unit,
    onJoinClick: () -> Unit,
    onInviteAccept: (TeamRequest) -> Unit,
    onInviteReject: (TeamRequest) -> Unit,
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
                Text(text = "Team erstellen")
            }
            Spacer(modifier = Modifier.width(8.dp))
            OutlinedButton(onClick = onJoinClick) {
                Text(text = "Team beitreten")
            }
        }
        if (teams.isEmpty()) {
            Text(
                text = "Du bist noch in keinem Team",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            Text(
                text = "Meine Teams",
                modifier = Modifier.fieldTopPadding(20),
                style = MaterialTheme.typography.titleMedium
            )
            Column(
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                filteredTeams.forEach { team ->
                    TeamCompactRow(
                        team = team,
                        onTeamClick = { onTeamClick(team) }
                    )
                }
            }
        }

        if (openInvites.isNotEmpty() || openJoinRequests.isNotEmpty()) {
            Text(
                text = openRequestTitle(openInvites, openJoinRequests),
                modifier = Modifier.fieldTopPadding(16),
                style = MaterialTheme.typography.titleMedium
            )
            RequestList(
                joinRequests = openJoinRequests,
                invites = openInvites,
                teamName = teamName,
                onInviteAccept = onInviteAccept,
                onRequestAccept = null,
                onRequestReject = onInviteReject
            )
        }
    }
}

@Composable
private fun TeamCompactRow(
    team: Team,
    onTeamClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(bottom = 8.dp)
            .fillMaxWidth()
            .clickable(onClick = onTeamClick)
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = team.name, style = MaterialTheme.typography.titleMedium)
                Text(
                    text = "${team.members.size} Mitglieder",
                    modifier = Modifier.fieldTopPadding(2),
                    style = MaterialTheme.typography.bodyMedium
                )
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
    joinRequests: List<TeamRequest>,
    invites: List<TeamRequest>,
    memberSuggestions: List<UserSearchResult>,
    onBackClick: () -> Unit,
    onMemberSearch: (String) -> Unit,
    onMemberInvite: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    onRequestAccept: (TeamRequest) -> Unit,
    onRequestReject: (TeamRequest) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    onTeamRemove: () -> Unit,
    onEventCreate: (TeamEvent, Boolean) -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
    onDutyCreate: (Duty) -> Unit,
    onDutyRemove: (Duty) -> Unit,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit,
    onFairPlanCreate: (TeamEvent, Boolean) -> Unit,
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
            onMemberInvite = onMemberInvite,
            onMemberRemove = onMemberRemove,
            joinRequests = joinRequests,
            invites = invites,
            memberSuggestions = memberSuggestions,
            onRequestAccept = onRequestAccept,
            onRequestReject = onRequestReject,
            onMemberSearch = onMemberSearch,
            onInviteCodeRefresh = onInviteCodeRefresh,
            onInviteCodeDeactivate = onInviteCodeDeactivate,
            onTeamRemove = onTeamRemove,
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
    onMemberSearch: (String) -> Unit,
    onMemberInvite: (String) -> Unit,
    onMemberRemove: (TeamMember) -> Unit,
    joinRequests: List<TeamRequest>,
    invites: List<TeamRequest>,
    memberSuggestions: List<UserSearchResult>,
    onRequestAccept: (TeamRequest) -> Unit,
    onRequestReject: (TeamRequest) -> Unit,
    onInviteCodeRefresh: () -> Unit,
    onInviteCodeDeactivate: () -> Unit,
    onTeamRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    var memberName by remember { mutableStateOf("") }
    var selectedUserId by remember { mutableStateOf("") }
    var memberError by remember { mutableStateOf("") }
    var showRemoveTeamDialog by remember { mutableStateOf(false) }

    if (tab == TeamDetailTab.Members) {
        Column(
            modifier = modifier
                .fillMaxSize()
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            ScreenHeader(title = team.name, onBackClick = onBackClick)
            TeamDetailTabs(selectedTab = tab, onTabSelect = onTabSelect)

            if (canManageTeam) {
                Text(
                    text = "Neues Mitglied einladen",
                    modifier = Modifier.fieldTopPadding(16),
                    style = MaterialTheme.typography.titleMedium
                )
                AuthTextField(
                    value = memberName,
                    onValueChange = {
                        memberName = it
                        selectedUserId = ""
                        memberError = ""
                        onMemberSearch(it)
                    },
                    label = "Name oder E-Mail",
                    modifier = Modifier.fieldTopPadding(12)
                )
                MemberSuggestions(
                    query = memberName,
                    existingMembers = team.members,
                    suggestions = memberSuggestions,
                    onSuggestionClick = { user ->
                        selectedUserId = user.id
                        memberName = user.label
                    }
                )
                if (memberError.isNotBlank()) {
                    ErrorMessage(
                        text = memberError,
                        modifier = Modifier.fieldTopPadding(8)
                    )
                }
                Button(
                    onClick = {
                        val trimmedName = memberName.trim()

                        if (trimmedName.isBlank()) {
                            memberError = "Bitte einen Namen eingeben"
                        } else if (selectedUserId.isBlank()) {
                            memberError = "Bitte einen Nutzer aus der Liste auswÃ¤hlen"
                        } else if (team.members.any { it.name == trimmedName }) {
                            memberError = "Dieses Mitglied ist schon im Team"
                        } else {
                            onMemberInvite(selectedUserId)
                            memberName = ""
                            selectedUserId = ""
                            memberError = ""
                        }
                    },
                    modifier = defaultActionModifier(topPadding = 12)
                ) {
                    Text(text = "Einladung senden")
                }
            }

            if (canManageTeam && (joinRequests.isNotEmpty() || invites.isNotEmpty())) {
                Text(
                    text = "Offene Vorgänge",
                    modifier = Modifier.fieldTopPadding(20),
                    style = MaterialTheme.typography.titleMedium
                )
                RequestList(
                    joinRequests = joinRequests,
                    invites = invites,
                    onRequestAccept = onRequestAccept,
                    onRequestReject = onRequestReject
                )
            }

            Text(
                text = "Mitglieder",
                modifier = Modifier.fieldTopPadding(20),
                style = MaterialTheme.typography.titleMedium
            )
            MemberList(
                members = team.members,
                canRemove = canManageTeam,
                onMemberRemove = onMemberRemove
            )
        }
        return
    }

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
                OutlinedButton(
                    onClick = { showRemoveTeamDialog = true },
                    modifier = defaultActionModifier(topPadding = 12)
                ) {
                    Text(text = "Team auflösen")
                }
            }
        }
    }

    if (showRemoveTeamDialog) {
        AlertDialog(
            onDismissRequest = { showRemoveTeamDialog = false },
            title = { Text(text = "Team auflösen?") },
            text = { Text(text = "Das Team und alle zugehörigen Termine, Dienste und Pläne werden gelöscht.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRemoveTeamDialog = false
                        onTeamRemove()
                    }
                ) {
                    Text(text = "Auflösen")
                }
            },
            dismissButton = {
                TextButton(onClick = { showRemoveTeamDialog = false }) {
                    Text(text = "Abbrechen")
                }
            }
        )
    }
}

@Composable
private fun TeamDetailTabs(
    selectedTab: TeamDetailTab,
    onTabSelect: (TeamDetailTab) -> Unit
) {
    val firstRow = listOf(TeamDetailTab.Overview, TeamDetailTab.Members, TeamDetailTab.Events)
    val secondRow = listOf(TeamDetailTab.Duties, TeamDetailTab.Plan)

    Column(modifier = Modifier.fieldTopPadding(16)) {
        TeamDetailTabRow(
            tabs = firstRow,
            selectedTab = selectedTab,
            onTabSelect = onTabSelect
        )
        TeamDetailTabRow(
            tabs = secondRow,
            selectedTab = selectedTab,
            onTabSelect = onTabSelect
        )
    }
}

@Composable
private fun TeamDetailTabRow(
    tabs: List<TeamDetailTab>,
    selectedTab: TeamDetailTab,
    onTabSelect: (TeamDetailTab) -> Unit
) {
    Row(
        modifier = Modifier
            .padding(top = 4.dp)
            .fillMaxWidth()
    ) {
        tabs.forEach { tab ->
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
    suggestions: List<UserSearchResult>,
    onSuggestionClick: (UserSearchResult) -> Unit
) {
    val trimmedQuery = query.trim()

    if (trimmedQuery.length < 2) {
        return
    }

    val filteredSuggestions = suggestions
        .filter { suggestion -> suggestion.label.contains(trimmedQuery, ignoreCase = true) }
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
            Text(text = suggestion.label)
        }
    }
}

@Composable
private fun TeamRequestCard(
    title: String,
    subtitle: String,
    onAcceptClick: (() -> Unit)?,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(top = 8.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = subtitle,
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
            Row(modifier = Modifier.fieldTopPadding(12)) {
                if (onAcceptClick != null) {
                    Button(onClick = onAcceptClick) {
                        Text(text = "Annehmen")
                    }
                    Spacer(modifier = Modifier.width(8.dp))
                }
                OutlinedButton(onClick = onRejectClick) {
                    Text(text = "Ablehnen")
                }
            }
        }
    }
}

@Composable
private fun RequestList(
    joinRequests: List<TeamRequest>,
    invites: List<TeamRequest>,
    teamName: ((String) -> String)? = null,
    onInviteAccept: ((TeamRequest) -> Unit)? = null,
    onRequestAccept: ((TeamRequest) -> Unit)?,
    onRequestReject: (TeamRequest) -> Unit
) {
    Column(
        modifier = Modifier
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        joinRequests.forEach { request ->
            CompactRequestRow(
                title = requestTitle(request, teamName),
                subtitle = if (teamName == null) {
                    "möchte beitreten"
                } else {
                    "Anfrage wartet auf Trainer"
                },
                acceptLabel = "Annehmen",
                rejectLabel = if (teamName == null) "Ablehnen" else "Abbrechen",
                onAcceptClick = onRequestAccept?.let { accept -> { accept(request) } },
                onRejectClick = { onRequestReject(request) }
            )
        }
        invites.forEach { invite ->
            CompactRequestRow(
                title = requestTitle(invite, teamName),
                subtitle = if (teamName == null) {
                    "Einladung offen"
                } else {
                    "Einladung zum Team"
                },
                acceptLabel = "Annehmen",
                rejectLabel = if (teamName == null) "Zurückziehen" else "Ablehnen",
                onAcceptClick = onInviteAccept?.let { accept -> { accept(invite) } },
                onRejectClick = { onRequestReject(invite) }
            )
        }
    }
}

@Composable
private fun CompactRequestRow(
    title: String,
    subtitle: String,
    acceptLabel: String,
    rejectLabel: String,
    onAcceptClick: (() -> Unit)?,
    onRejectClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .padding(top = 6.dp)
            .fillMaxWidth()
    ) {
        Row(
            modifier = Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(text = title, style = MaterialTheme.typography.titleSmall)
                Text(
                    text = subtitle,
                    modifier = Modifier.fieldTopPadding(2),
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            if (onAcceptClick != null) {
                Button(onClick = onAcceptClick) {
                    Text(text = acceptLabel)
                }
                Spacer(modifier = Modifier.width(6.dp))
            }
            OutlinedButton(onClick = onRejectClick) {
                Text(text = rejectLabel)
            }
        }
    }
}

private fun openRequestTitle(
    openInvites: List<TeamRequest>,
    openJoinRequests: List<TeamRequest>
): String {
    return when {
        openInvites.isNotEmpty() && openJoinRequests.isNotEmpty() -> "Offene Anfragen und Einladungen"
        openJoinRequests.isNotEmpty() -> "Offene Anfragen"
        else -> "Offene Einladungen"
    }
}

private fun requestTitle(
    request: TeamRequest,
    teamName: ((String) -> String)?
): String {
    return if (teamName == null) {
        request.userName
    } else {
        request.teamName.ifBlank { teamName(request.teamId) }
    }
}

@Composable
private fun MemberList(
    members: List<TeamMember>,
    canRemove: Boolean,
    onMemberRemove: (TeamMember) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .padding(top = 8.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        members.forEach { member ->
            TeamMemberRow(
                member = member,
                canRemove = canRemove,
                onRemoveClick = { onMemberRemove(member) }
            )
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
        if (errorText.isNotBlank()) {
            ErrorMessage(text = errorText, modifier = Modifier.fieldTopPadding(12))
        }
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
        OutlinedButton(onClick = onBackClick, modifier = defaultActionModifier(topPadding = 12)) {
            Text(text = "Zurück")
        }
    }
}

@Composable
private fun JoinTeamContent(
    onJoinClick: (String, (String?) -> Unit) -> Unit,
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
        if (errorText.isNotBlank()) {
            ErrorMessage(text = errorText, modifier = Modifier.fieldTopPadding(12))
        }
        Button(
            onClick = {
                val trimmedCode = inviteCode.trim()

                if (trimmedCode.isBlank()) {
                    errorText = "Bitte einen Invite-Code eingeben"
                } else {
                    onJoinClick(trimmedCode) { error ->
                        errorText = error.orEmpty()
                    }
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Beitreten")
        }
        OutlinedButton(onClick = onBackClick, modifier = defaultActionModifier(topPadding = 12)) {
            Text(text = "Zurück")
        }
    }
}
