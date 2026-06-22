package de.teamplaner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
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
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.NavigationBar
import androidx.compose.material3.NavigationBarItem
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import de.teamplaner.model.TeilnahmeStatus
import de.teamplaner.model.UserSearchResult
import de.teamplaner.ui.state.MainAppState
import kotlinx.coroutines.delay
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
    var isLoadingRemoteData by remember { mutableStateOf(false) }
    var memberSuggestions by remember { mutableStateOf<List<UserSearchResult>>(emptyList()) }
    var selectedTeamId by remember(token) { mutableStateOf<String?>(null) }
    val usesBackend = token.isNotBlank()

    fun reloadRemoteTeams() {
        reloadTeams += 1
    }

    LaunchedEffect(token, reloadTeams) {
        if (usesBackend) {
            isLoadingRemoteData = true
            teamApiClient.loadData(token)
                .onSuccess {
                    remoteData = it
                    remoteError = ""
                }
                .onFailure {
                    remoteError = it.message ?: "Teams konnten nicht geladen werden"
                }
            isLoadingRemoteData = false
        }
    }

    LaunchedEffect(token, usesBackend) {
        while (usesBackend) {
            delay(15000)
            reloadRemoteTeams()
        }
    }

    val localRepository = remember(context, displayName) {
        LocalTeamPlanerRepository(context, displayName)
    }
    val appState = remember(displayName, context, remoteData, usesBackend) {
        MainAppState(
            displayName = displayName,
            repository = localRepository,
            initialSelectedTeamId = selectedTeamId,
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
            isLoadingRemoteData = true
            action()
                .onSuccess { reloadRemoteTeams() }
                .onFailure { remoteError = it.message ?: "Aktion fehlgeschlagen" }
            isLoadingRemoteData = false
        }
    }

    fun selectTeam(teamId: String) {
        selectedTeamId = teamId
        appState.selectTeam(teamId)
    }

    fun searchMembers(query: String) {
        val trimmedQuery = query.trim()

        if (trimmedQuery.length < 2) {
            memberSuggestions = emptyList()
            return
        }

        if (!usesBackend) {
            memberSuggestions = emptyList()
            return
        }

        scope.launch {
            teamApiClient.searchUsers(token, trimmedQuery, appState.selectedTeam?.id)
                .onSuccess { users -> memberSuggestions = users }
                .onFailure {
                    memberSuggestions = emptyList()
                    remoteError = it.message ?: "Nutzer konnten nicht gesucht werden"
                }
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
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .fillMaxSize()
        ) {
            BackendStatusBar(
                usesBackend = usesBackend,
                isLoading = isLoadingRemoteData,
                errorText = remoteError,
                onRefreshClick = { reloadRemoteTeams() }
            )
            Box(modifier = Modifier.weight(1f)) {
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
                memberSuggestions = memberSuggestions,
                teamName = appState::teamName,
                onTeamSelect = ::selectTeam,
                onTeamCreate = { teamName ->
                    if (usesBackend) {
                        scope.launch {
                            isLoadingRemoteData = true
                            teamApiClient.createTeam(token, teamName)
                                .onSuccess { team ->
                                    selectedTeamId = team.id
                                    reloadRemoteTeams()
                                }
                                .onFailure {
                                    remoteError = it.message ?: "Team konnte nicht erstellt werden"
                                }
                            isLoadingRemoteData = false
                        }
                    } else {
                        appState.createTeam(teamName)
                        selectedTeamId = appState.selectedTeam?.id
                    }
                },
                onTeamJoin = { inviteCode, onResult ->
                    if (usesBackend) {
                        scope.launch {
                            isLoadingRemoteData = true
                            teamApiClient.joinTeam(token, inviteCode)
                                .onSuccess {
                                    onResult(null)
                                    reloadRemoteTeams()
                                }
                                .onFailure {
                                    onResult("Invite-Code nicht gültig")
                                }
                            isLoadingRemoteData = false
                        }
                    } else {
                        appState.joinTeam(inviteCode)
                        onResult(null)
                    }
                },
                onMemberSearch = ::searchMembers,
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
                onTeamRemove = {
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.removeTeam(token, teamId) }
                    } else {
                        appState.removeSelectedTeam()
                    }
                },
                onEventCreate = { event, shouldAutoAssign ->
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        scope.launch {
                            isLoadingRemoteData = true
                            teamApiClient.createEvent(token, teamId, event)
                                .onSuccess {
                                    if (shouldAutoAssign) {
                                        teamApiClient.createFairPlan(token, teamId, false)
                                            .onFailure { error ->
                                                remoteError = error.message ?: "Plan konnte nicht erstellt werden"
                                            }
                                    }
                                    reloadRemoteTeams()
                                }
                                .onFailure {
                                    remoteError = it.message ?: "Termin konnte nicht gespeichert werden"
                                }
                            isLoadingRemoteData = false
                        }
                    } else {
                        appState.createEvent(event)
                        if (shouldAutoAssign) {
                            appState.createFairPlan(false)
                        }
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
                onDutyAssign = { event, duty, member ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.assignDuty(token, event, duty, member) }
                    } else {
                        appState.assignDuty(event, duty, member)
                    }
                },
                onAssignmentRemove = { assignment ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.removeAssignment(token, assignment.id) }
                    } else {
                        appState.removeAssignment(assignment)
                    }
                },
                onFairPlanCreate = { replaceExisting ->
                    val teamId = appState.selectedTeam?.id
                    if (usesBackend && teamId != null) {
                        runTeamAction { teamApiClient.createFairPlan(token, teamId, replaceExisting) }
                    } else {
                        appState.createFairPlan(replaceExisting)
                    }
                },
                modifier = Modifier
            )
                    AppTab.Events -> GlobalEventScreen(
                teams = appState.teams,
                events = appState.events,
                currentName = displayName,
                onEventUpdate = { oldEvent, newEvent ->
                    if (usesBackend) {
                        runTeamAction { teamApiClient.updateEvent(token, newEvent.copy(id = oldEvent.id)) }
                    } else {
                        appState.updateEvent(oldEvent, newEvent)
                    }
                },
                modifier = Modifier
            )
                    AppTab.Plan -> GlobalPlanScreen(
                teams = appState.teams,
                events = appState.events,
                duties = appState.duties,
                currentName = displayName,
                assignments = appState.assignments,
                canManageTeam = appState::canManageTeam,
                modifier = Modifier
            )
                    AppTab.Profile -> ProfileScreen(
                name = displayName,
                teams = appState.teams,
                modifier = Modifier
            )
                }
            }
        }
    }
}

@Composable
private fun BackendStatusBar(
    usesBackend: Boolean,
    isLoading: Boolean,
    errorText: String,
    onRefreshClick: () -> Unit
) {
    if (!usesBackend || (!isLoading && errorText.isBlank())) {
        return
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (isLoading) {
            LinearProgressIndicator(
                modifier = Modifier
                    .widthIn(max = 520.dp)
                    .fillMaxWidth()
            )
        }
        if (errorText.isNotBlank()) {
            ErrorMessage(
                text = errorText,
                modifier = Modifier
                    .padding(top = 8.dp)
                    .widthIn(max = 520.dp)
            )
            TextButton(onClick = onRefreshClick) {
                Text(text = "Erneut versuchen")
            }
        }
    }
}

@Composable
private fun GlobalEventScreen(
    teams: List<Team>,
    events: List<TeamEvent>,
    currentName: String,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var showUpcoming by remember { mutableStateOf(true) }
    var selectedEventId by remember { mutableStateOf<String?>(null) }
    val selectedEvent = events.firstOrNull { it.id == selectedEventId }
    val selectedTeam = selectedEvent?.let { event -> teams.firstOrNull { it.id == event.teamId } }
    val visibleTeamIds = teams.map { it.id }.toSet()
    val filteredEvents = events.filter { event ->
        event.teamId in visibleTeamIds &&
            if (showUpcoming) !event.date.startsWith("0") else event.date.startsWith("0")
    }

    if (selectedEvent != null && selectedTeam != null) {
        GlobalEventDetailScreen(
            team = selectedTeam,
            event = selectedEvent,
            currentName = currentName,
            onBackClick = { selectedEventId = null },
            onEventUpdate = onEventUpdate,
            modifier = modifier
        )
        return
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
                        .clickable { selectedEventId = event.id }
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
private fun GlobalEventDetailScreen(
    team: Team,
    event: TeamEvent,
    currentName: String,
    onBackClick: () -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    val currentMember = team.members.firstOrNull { it.name == currentName }
    val ownParticipation = event.teilnahmen.firstOrNull { it.memberId == currentMember?.id }
    val offene = event.teilnahmen.count { it.status == TeilnahmeStatus.Offen }
    val zugesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Zugesagt }
    val abgesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Abgesagt }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        ScreenHeader(
            title = "Termindetails",
            onBackClick = onBackClick
        )
        Text(
            text = event.title,
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.headlineMedium
        )
        Text(
            text = team.name,
            modifier = Modifier.fieldTopPadding(8),
            style = MaterialTheme.typography.titleMedium
        )
        Text(
            text = "${event.date} um ${event.time}",
            modifier = Modifier.fieldTopPadding(16),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = event.location.ifBlank { "Kein Ort angegeben" },
            modifier = Modifier.fieldTopPadding(8),
            style = MaterialTheme.typography.bodyLarge
        )
        Text(
            text = "Status: $zugesagt zugesagt, $abgesagt abgesagt, $offene offen",
            modifier = Modifier.fieldTopPadding(16),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Meine Teilnahme",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        if (ownParticipation == null) {
            Text(
                text = "Du bist bei diesem Termin nicht als Teilnehmer eingetragen.",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            androidx.compose.foundation.layout.Row(
                modifier = Modifier.fieldTopPadding(8)
            ) {
                TeilnahmeStatus.entries.forEach { status ->
                    androidx.compose.material3.FilterChip(
                        selected = ownParticipation.status == status,
                        onClick = {
                            onEventUpdate(
                                event,
                                event.copy(
                                    teilnahmen = event.teilnahmen.map { teilnahme ->
                                        if (teilnahme.memberId == ownParticipation.memberId) {
                                            teilnahme.copy(status = status)
                                        } else {
                                            teilnahme
                                        }
                                    }
                                )
                            )
                        },
                        label = { Text(text = status.label) },
                        modifier = Modifier.padding(end = 8.dp)
                    )
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
