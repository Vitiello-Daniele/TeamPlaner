package de.teamplaner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.teamplaner.R
import de.teamplaner.data.LocalTeamPlanerRepository
import de.teamplaner.data.TeamPlanerData
import de.teamplaner.data.team.TeamApiClient
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRequestStatus
import de.teamplaner.ui.state.MainAppState
import kotlinx.coroutines.launch

private enum class AppTab(val title: String) {
    Teams("Teams"),
    Events("Termine"),
    Plan("Plan"),
    Profile("Profil")
}

@Composable
@OptIn(ExperimentalMaterial3Api::class)
fun MainAppScreen(
    name: String,
    token: String,
    onLogoutClick: () -> Unit
) {
    val displayName = name.ifBlank { "Kein Name angegeben" }
    val context = LocalContext.current.applicationContext
    val scope = rememberCoroutineScope()
    val teamApiClient = remember { TeamApiClient() }
    var selectedTab by remember { mutableStateOf(AppTab.Teams) }
    var reloadTeams by remember { mutableStateOf(0) }
    var remoteData by remember(token) { mutableStateOf<TeamPlanerData?>(null) }
    var remoteError by remember { mutableStateOf("") }
    val usesBackend = token.isNotBlank()

    fun reloadRemoteTeams() {
        reloadTeams += 1
    }

    LaunchedEffect(token, reloadTeams) {
        if (usesBackend) {
            teamApiClient.loadData(token)
                .onSuccess {
                    remoteData = it
                    remoteError = ""
                }
                .onFailure {
                    remoteError = it.message ?: "Teams konnten nicht geladen werden"
                }
        }
    }

    val localRepository = remember(context, displayName) {
        LocalTeamPlanerRepository(context, displayName)
    }
    val appState = remember(displayName, context, remoteData, usesBackend) {
        MainAppState(
            displayName = displayName,
            repository = localRepository,
            initialData = remoteData ?: if (usesBackend) {
                TeamPlanerData(
                    teams = emptyList(),
                    events = emptyList(),
                    duties = emptyList(),
                    assignments = emptyList(),
                    requests = emptyList()
                )
            } else {
                localRepository.loadData(displayName)
            }
        )
    }

    fun runTeamAction(action: suspend () -> Result<Unit>) {
        scope.launch {
            action()
                .onSuccess { reloadRemoteTeams() }
                .onFailure { remoteError = it.message ?: "Aktion fehlgeschlagen" }
        }
    }

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
            AppTab.Teams -> TeamScreen(
                teams = appState.teams,
                selectedTeam = appState.selectedTeam,
                currentMember = appState.currentMember,
                canManageSelectedTeam = appState.isTrainer,
                selectedTeamEvents = appState.selectedTeam?.let { appState.teamEvents(it.id) }.orEmpty(),
                selectedTeamDuties = appState.selectedTeam?.let { appState.teamDuties(it.id) }.orEmpty(),
                selectedTeamAssignments = appState.selectedTeam?.let { appState.teamAssignments(it.id) }.orEmpty(),
                openInvites = appState.openInvites,
                openJoinRequests = appState.openJoinRequests,
                selectedTeamJoinRequests = appState.selectedTeam?.let { appState.openJoinRequests(it.id) }.orEmpty(),
                selectedTeamInvites = appState.selectedTeam?.let { appState.openInvites(it.id) }.orEmpty(),
                teamName = appState::teamName,
                onTeamSelect = appState::selectTeam,
                onTeamCreate = { teamName ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.createTeam(token, teamName) }
                    } else {
                        appState.createTeam(teamName)
                    }
                },
                onTeamJoin = { inviteCode ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.joinTeam(token, inviteCode) }
                    } else {
                        appState.joinTeam(inviteCode)
                    }
                },
                onMemberInvite = { user ->
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.inviteMember(token, teamId, user) }
                    } else {
                        appState.inviteMember(user)
                    }
                },
                onMemberRemove = { member ->
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.removeMember(token, teamId, member.id) }
                    } else {
                        appState.removeMember(member)
                    }
                },
                onRequestAccept = { request ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.updateRequest(token, request.id, TeamRequestStatus.Accepted) }
                    } else {
                        appState.acceptRequest(request)
                    }
                },
                onRequestReject = { request ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.updateRequest(token, request.id, TeamRequestStatus.Rejected) }
                    } else {
                        appState.rejectRequest(request)
                    }
                },
                onInviteCodeRefresh = {
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.refreshInviteCode(token, teamId) }
                    } else {
                        appState.refreshInviteCode()
                    }
                },
                onInviteCodeDeactivate = {
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.deactivateInviteCode(token, teamId) }
                    } else {
                        appState.deactivateInviteCode()
                    }
                },
                onEventCreate = { event ->
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.createEvent(token, teamId, event) }
                    } else {
                        appState.createEvent(event)
                    }
                },
                onEventUpdate = { oldEvent, newEvent ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.updateEvent(token, newEvent.copy(id = oldEvent.id)) }
                    } else {
                        appState.updateEvent(oldEvent, newEvent)
                    }
                },
                onEventRemove = { event ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.removeEvent(token, event.id) }
                    } else {
                        appState.removeEvent(event)
                    }
                },
                onDutyCreate = { duty ->
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.createDuty(token, teamId, duty) }
                    } else {
                        appState.createDuty(duty)
                    }
                },
                onDutyRemove = { duty ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.removeDuty(token, duty.id) }
                    } else {
                        appState.removeDuty(duty)
                    }
                },
                onDutyAssign = appState::assignDuty,
                onAssignmentRemove = appState::removeAssignment,
                onFairPlanCreate = appState::createFairPlan,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.Events -> GlobalEventScreen(
                teams = appState.teams,
                events = appState.events,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.Plan -> GlobalPlanScreen(
                teams = appState.teams,
                events = appState.events,
                duties = appState.duties,
                currentName = displayName,
                assignments = appState.assignments,
                canManageTeam = appState::canManageTeam,
                modifier = Modifier.padding(innerPadding)
            )
            AppTab.Profile -> ProfileScreen(
                name = displayName,
                teams = appState.teams,
                modifier = Modifier.padding(innerPadding)
            )
        }
    }
}

@Composable
private fun GlobalEventScreen(
    teams: List<Team>,
    events: List<TeamEvent>,
    modifier: Modifier = Modifier
) {
    var showUpcoming by remember { mutableStateOf(true) }
    val visibleTeamIds = teams.map { it.id }.toSet()
    val filteredEvents = events.filter { event ->
        event.teamId in visibleTeamIds &&
            if (showUpcoming) !event.date.startsWith("0") else event.date.startsWith("0")
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Termine", style = MaterialTheme.typography.headlineMedium)
        androidx.compose.foundation.layout.Row(modifier = Modifier.fieldTopPadding(16)) {
            androidx.compose.material3.FilterChip(
                selected = showUpcoming,
                onClick = { showUpcoming = true },
                label = { Text("Bevorstehend") },
                modifier = Modifier.padding(end = 8.dp)
            )
            androidx.compose.material3.FilterChip(
                selected = !showUpcoming,
                onClick = { showUpcoming = false },
                label = { Text("Vergangen") }
            )
        }
        if (filteredEvents.isEmpty()) {
            Text(
                text = "Keine Termine gefunden",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            filteredEvents.forEach { event ->
                val teamName = teams.firstOrNull { it.id == event.teamId }?.name ?: "Unbekanntes Team"
                Card(
                    modifier = Modifier
                        .padding(top = 12.dp)
                        .widthIn(max = 520.dp)
                        .fillMaxWidth()
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(text = teamName, style = MaterialTheme.typography.labelLarge)
                        Text(text = event.title, modifier = Modifier.fieldTopPadding(4), style = MaterialTheme.typography.titleMedium)
                        Text(text = "${event.date} um ${event.time}", modifier = Modifier.fieldTopPadding(4))
                        Text(text = event.location.ifBlank { "Kein Ort angegeben" }, modifier = Modifier.fieldTopPadding(4))
                    }
                }
            }
        }
    }
}

@Composable
private fun GlobalPlanScreen(
    teams: List<Team>,
    events: List<TeamEvent>,
    duties: List<de.teamplaner.model.Duty>,
    currentName: String,
    assignments: List<DutyAssignment>,
    canManageTeam: (Team) -> Boolean,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(text = "Plan", style = MaterialTheme.typography.headlineMedium)

        teams.forEach { team ->
            val teamEventIds = events.filter { it.teamId == team.id }.map { it.id }.toSet()
            val teamAssignments = assignments.filter { it.eventId in teamEventIds }
            val ownMember = team.members.firstOrNull { it.name == currentName }
            val ownAssignments = teamAssignments.filter { it.memberId == ownMember?.id }

            Card(
                modifier = Modifier
                    .padding(top = 16.dp)
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(text = team.name, style = MaterialTheme.typography.titleMedium)
                    if (ownAssignments.isEmpty()) {
                        Text(
                            text = "Keine eigenen Dienste",
                            modifier = Modifier.fieldTopPadding(8),
                            style = MaterialTheme.typography.bodyMedium
                        )
                    } else {
                        ownAssignments.forEach { assignment ->
                            GlobalAssignmentCard(
                                assignment = assignment,
                                events = events,
                                duties = duties,
                                teams = teams,
                                isOwnAssignment = true
                            )
                        }
                    }
                    if (canManageTeam(team) && teamAssignments.isNotEmpty()) {
                        Text(
                            text = "Trainerübersicht",
                            modifier = Modifier.fieldTopPadding(16),
                            style = MaterialTheme.typography.titleSmall
                        )
                        teamAssignments.forEach { assignment ->
                            GlobalAssignmentCard(
                                assignment = assignment,
                                events = events,
                                duties = duties,
                                teams = teams,
                                isOwnAssignment = assignment.memberId == ownMember?.id,
                                showMember = true
                            )
                        }
                    }
                }
            }
        }

        if (teams.isEmpty()) {
            Text(
                text = "Du bist noch in keinem Team",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
private fun GlobalAssignmentCard(
    assignment: DutyAssignment,
    events: List<TeamEvent>,
    duties: List<de.teamplaner.model.Duty>,
    teams: List<Team>,
    isOwnAssignment: Boolean,
    showMember: Boolean = false
) {
    val event = events.firstOrNull { it.id == assignment.eventId }
    val duty = duties.firstOrNull { it.id == assignment.dutyId }
    val member = teams
        .asSequence()
        .flatMap { it.members.asSequence() }
        .firstOrNull { it.id == assignment.memberId }

    Card(
        colors = if (isOwnAssignment) {
            CardDefaults.cardColors(containerColor = Color(0xFFDFF5E6))
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier
            .padding(top = 8.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = event?.title ?: "Unbekannter Termin",
                style = MaterialTheme.typography.titleSmall
            )
            Text(
                text = if (event == null) "Kein Datum" else "${event.date} um ${event.time}",
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (showMember) {
                    "${duty?.title ?: "Unbekannter Dienst"} - ${member?.name ?: "Unbekanntes Mitglied"}"
                } else {
                    duty?.title ?: "Unbekannter Dienst"
                },
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}
