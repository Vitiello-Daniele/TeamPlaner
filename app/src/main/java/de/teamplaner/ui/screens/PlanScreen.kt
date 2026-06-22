package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember

private sealed interface PlanView {
    data object List : PlanView
    data class Form(val assignment: DutyAssignment?) : PlanView
}

@Composable
fun PlanScreen(
    team: Team?,
    currentMember: TeamMember?,
    events: List<TeamEvent>,
    duties: List<Duty>,
    assignments: List<DutyAssignment>,
    canManageAssignments: Boolean,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit,
    onFairPlanCreate: (Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    var planView by remember { mutableStateOf<PlanView>(PlanView.List) }

    when (val currentView = planView) {
        PlanView.List -> PlanListScreen(
            team = team,
            events = events,
            duties = duties,
            assignments = assignments,
            canManageAssignments = canManageAssignments,
            currentMember = currentMember,
            onCreateClick = { planView = PlanView.Form(assignment = null) },
            onAssignmentEdit = { assignment -> planView = PlanView.Form(assignment) },
            onFairPlanCreate = onFairPlanCreate,
            onAssignmentRemove = onAssignmentRemove,
            modifier = modifier
        )
        is PlanView.Form -> AssignmentFormScreen(
            team = team,
            events = events,
            duties = duties,
            editedAssignment = currentView.assignment,
            onBackClick = { planView = PlanView.List },
            onDutyAssign = { event, duty, member ->
                onDutyAssign(event, duty, member)
                planView = PlanView.List
            },
            modifier = modifier
        )
    }
}

@Composable
private fun PlanListScreen(
    team: Team?,
    events: List<TeamEvent>,
    duties: List<Duty>,
    assignments: List<DutyAssignment>,
    canManageAssignments: Boolean,
    currentMember: TeamMember?,
    onCreateClick: () -> Unit,
    onAssignmentEdit: (DutyAssignment) -> Unit,
    onFairPlanCreate: (Boolean) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            text = "Plan",
            style = MaterialTheme.typography.headlineMedium
        )

        if (team == null) {
            Text(
                text = "Erstelle oder betrete zuerst ein Team",
                modifier = Modifier.fieldTopPadding(12),
                style = MaterialTheme.typography.bodyLarge
            )
            return@Column
        }

        PlanSummary(
            team = team,
            events = events,
            duties = duties,
            assignments = assignments
        )
        MyAssignments(
            currentMember = currentMember,
            events = events,
            duties = duties,
            assignments = assignments
        )

        if (canManageAssignments) {
            AutoPlanHints(
                team = team,
                events = events,
                duties = duties
            )
            Button(
                onClick = onCreateClick,
                modifier = defaultActionModifier(topPadding = 24)
            ) {
                Text(text = "Dienst zuweisen")
            }
            Button(
                onClick = { onFairPlanCreate(false) },
                modifier = defaultActionModifier(topPadding = 12)
            ) {
                Text(text = "Automatisch ergänzen")
            }
            OutlinedButton(
                onClick = { onFairPlanCreate(true) },
                modifier = defaultActionModifier(topPadding = 12)
            ) {
                Text(text = "Plan neu verteilen")
            }
        }

        if (events.isEmpty()) {
            Text(
                text = "Noch keine Termine geplant",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            events.forEach { event ->
                val eventDuties = dutiesForEvent(event, duties)
                EventAssignmentCard(
                    event = event,
                    duties = eventDuties,
                    assignments = assignments.filter { it.eventId == event.id },
                    team = team,
                    events = events,
                    currentMember = currentMember,
                    canManageAssignments = canManageAssignments,
                    onAssignmentEdit = onAssignmentEdit,
                    onAssignmentRemove = onAssignmentRemove
                )
            }
        }
    }
}

@Composable
private fun AutoPlanHints(
    team: Team,
    events: List<TeamEvent>,
    duties: List<Duty>
) {
    val hint = when {
        team.members.isEmpty() -> "Automatische Verteilung braucht mindestens ein Mitglied."
        events.isEmpty() -> "Automatische Verteilung braucht mindestens einen Termin."
        duties.isEmpty() -> "Automatische Verteilung braucht mindestens einen Dienst."
        else -> ""
    }

    if (hint.isNotBlank()) {
        Text(
            text = hint,
            modifier = Modifier.fieldTopPadding(16),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun PlanSummary(
    team: Team,
    events: List<TeamEvent>,
    duties: List<Duty>,
    assignments: List<DutyAssignment>
) {
    Card(
        modifier = Modifier
            .padding(top = 24.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Übersicht",
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${events.size} Termine, ${duties.size} Dienste, ${assignments.size} Zuweisungen",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = planStatusText(
                    events = events,
                            duties = duties,
                            assignments = assignments
                ),
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "Automatische Verteilung bevorzugt Mitglieder mit weniger bisherigen Diensten.",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
            MemberLoadList(
                members = team.members,
                assignments = assignments
            )
        }
    }
}

@Composable
private fun MyAssignments(
    currentMember: TeamMember?,
    events: List<TeamEvent>,
    duties: List<Duty>,
    assignments: List<DutyAssignment>
) {
    if (currentMember == null) {
        return
    }

    val ownAssignments = assignments.filter { it.memberId == currentMember.id }

    Card(
        modifier = Modifier
            .padding(top = 16.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Meine Dienste",
                style = MaterialTheme.typography.titleMedium
            )
            if (ownAssignments.isEmpty()) {
                Text(
                    text = "Du hast aktuell keine Dienste",
                    modifier = Modifier.fieldTopPadding(8),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                ownAssignments.forEach { assignment ->
                    OwnAssignmentCard(
                        assignment = assignment,
                        events = events,
                        duties = duties
                    )
                }
            }
        }
    }
}

@Composable
private fun MemberLoadList(
    members: List<TeamMember>,
    assignments: List<DutyAssignment>
) {
    Text(
        text = "Dienste pro Mitglied",
        modifier = Modifier.fieldTopPadding(16),
        style = MaterialTheme.typography.titleSmall
    )
    members.forEach { member ->
        val count = assignments.count { it.memberId == member.id }

        Text(
            text = "${member.name}: $count",
            modifier = Modifier.fieldTopPadding(4),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}


@Composable
private fun EventAssignmentCard(
    event: TeamEvent,
    duties: List<Duty>,
    assignments: List<DutyAssignment>,
    team: Team,
    events: List<TeamEvent>,
    currentMember: TeamMember?,
    canManageAssignments: Boolean,
    onAssignmentEdit: (DutyAssignment) -> Unit,
    onAssignmentRemove: (DutyAssignment) -> Unit
) {
    val missingDuties = duties.filterNot { duty ->
        assignments.any { it.dutyId == duty.id }
    }

    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${event.date} um ${event.time}",
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (missingDuties.isEmpty()) {
                    "Plan vollständig"
                } else {
                    "Offen: ${missingDuties.joinToString { it.title }}"
                },
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )

            if (assignments.isEmpty()) {
                Text(
                    text = "Noch keine Dienste zugewiesen",
                    modifier = Modifier.fieldTopPadding(12),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                assignments.forEach { assignment ->
                    AssignmentLine(
                        assignment = assignment,
                        team = team,
                        events = events,
                        duties = duties,
                        isOwnAssignment = assignment.memberId == currentMember?.id
                    )
                    if (canManageAssignments) {
                        Button(
                            onClick = { onAssignmentEdit(assignment) },
                            modifier = defaultActionModifier(topPadding = 8)
                        ) {
                            Text(text = "Bearbeiten")
                        }
                        OutlinedButton(
                            onClick = { onAssignmentRemove(assignment) },
                            modifier = defaultActionModifier(topPadding = 8)
                        ) {
                            Text(text = "Zuweisung löschen")
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun OwnAssignmentCard(
    assignment: DutyAssignment,
    events: List<TeamEvent>,
    duties: List<Duty>
) {
    val event = events.firstOrNull { it.id == assignment.eventId }
    val duty = duties.firstOrNull { it.id == assignment.dutyId }

    Card(
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFDFF5E6)
        ),
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
                text = duty?.title ?: "Unbekannter Dienst",
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssignmentLine(
    assignment: DutyAssignment,
    team: Team,
    events: List<TeamEvent>,
    duties: List<Duty>,
    isOwnAssignment: Boolean
) {
    val event = events.firstOrNull { it.id == assignment.eventId }
    val duty = duties.firstOrNull { it.id == assignment.dutyId }
    val member = team.members.firstOrNull { it.id == assignment.memberId }

    Card(
        colors = if (isOwnAssignment) {
            CardDefaults.cardColors(containerColor = Color(0xFFDFF5E6))
        } else {
            CardDefaults.cardColors()
        },
        modifier = Modifier
            .padding(top = 12.dp)
            .fillMaxWidth()
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = "${duty?.title ?: "Unbekannter Dienst"}: ${member?.name ?: "Unbekanntes Mitglied"}",
                style = MaterialTheme.typography.bodyLarge
            )
            Text(
                text = if (event == null) "Kein Datum" else "${event.date} um ${event.time}",
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun AssignmentFormScreen(
    team: Team?,
    events: List<TeamEvent>,
    duties: List<Duty>,
    editedAssignment: DutyAssignment?,
    onBackClick: () -> Unit,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEvent by remember(editedAssignment) {
        mutableStateOf(
            editedAssignment?.let { assignment ->
                events.firstOrNull { it.id == assignment.eventId }
            } ?: events.firstOrNull()
        )
    }
    var selectedDuty by remember(editedAssignment) {
        val firstEvent = editedAssignment?.let { assignment ->
            events.firstOrNull { it.id == assignment.eventId }
        } ?: events.firstOrNull()
        mutableStateOf(
            editedAssignment?.let { assignment ->
                duties.firstOrNull { it.id == assignment.dutyId }
            } ?: firstEvent?.let { dutiesForEvent(it, duties).firstOrNull() }
        )
    }
    var memberQuery by remember(editedAssignment) {
        mutableStateOf(
            editedAssignment?.let { assignment ->
                team?.members?.firstOrNull { it.id == assignment.memberId }?.name
            }.orEmpty()
        )
    }
    var selectedMember by remember(editedAssignment) {
        mutableStateOf(
            editedAssignment?.let { assignment ->
                team?.members?.firstOrNull { it.id == assignment.memberId }
            }
        )
    }
    var errorText by remember { mutableStateOf("") }
    val visibleDuties = selectedEvent?.let { event ->
        dutiesForEvent(event, duties)
    } ?: emptyList()

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = if (editedAssignment == null) "Dienst zuweisen" else "Zuweisung bearbeiten",
            onBackClick = onBackClick
        )

        if (team == null) {
            Text(
                text = "Erstelle oder betrete zuerst ein Team",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
            return@Column
        }

        Text(
            text = "Termin",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        if (events.isEmpty()) {
            Text(
                text = "Lege zuerst einen Termin an.",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        events.forEach { event ->
            FilterChip(
                selected = selectedEvent?.id == event.id,
                onClick = {
                    selectedEvent = event
                    selectedDuty = dutiesForEvent(event, duties).firstOrNull()
                },
                label = { Text(text = event.title) },
                modifier = Modifier.fieldTopPadding(8)
            )
        }

        Text(
            text = "Dienst",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        if (selectedEvent == null) {
            Text(
                text = "Wähle zuerst einen Termin aus.",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        } else if (visibleDuties.isEmpty()) {
            Text(
                text = "Für diesen Termin sind keine Dienste ausgewählt.",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        }
        visibleDuties.forEach { duty ->
            FilterChip(
                selected = selectedDuty?.id == duty.id,
                onClick = { selectedDuty = duty },
                label = { Text(text = duty.title) },
                modifier = Modifier.fieldTopPadding(8)
            )
        }

        AuthTextField(
            value = memberQuery,
            onValueChange = {
                memberQuery = it
                selectedMember = null
            },
            label = "Mitglied suchen",
            modifier = Modifier.fieldTopPadding(24)
        )
        AssignmentMemberSuggestions(
            query = memberQuery,
            members = team.members,
            onMemberClick = { member ->
                selectedMember = member
                memberQuery = member.name
            }
        )

        if (errorText.isNotBlank()) {
            ErrorMessage(
                text = errorText,
                modifier = Modifier.fieldTopPadding(12)
            )
        }

        Button(
            onClick = {
                val event = selectedEvent
                val duty = selectedDuty
                val member = selectedMember

                when {
                    events.isEmpty() -> errorText = "Bitte zuerst einen Termin anlegen"
                    event == null -> errorText = "Bitte einen Termin auswählen"
                    visibleDuties.isEmpty() -> errorText = "Bitte im Termin zuerst Dienste auswählen"
                    duty == null -> errorText = "Bitte einen Dienst auswählen"
                    member == null -> errorText = "Bitte ein Mitglied auswählen"
                    else -> onDutyAssign(event, duty, member)
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = if (editedAssignment == null) "Zuweisen" else "Speichern")
        }
    }
}

private fun planStatusText(
    events: List<TeamEvent>,
    duties: List<Duty>,
    assignments: List<DutyAssignment>
): String {
    val expectedAssignments = events.sumOf { event ->
        dutiesForEvent(event, duties).size
    }
    val missingAssignments = expectedAssignments - assignments.size

    return if (expectedAssignments == 0) {
        "Plan noch nicht bewertbar"
    } else if (missingAssignments <= 0) {
        "Plan vollständig"
    } else {
        "Plan unvollständig: $missingAssignments Zuweisungen fehlen"
    }
}

private fun dutiesForEvent(
    event: TeamEvent,
    duties: List<Duty>
): List<Duty> {
    return duties.filter { it.id in event.dutyIds }
}

@Composable
private fun AssignmentMemberSuggestions(
    query: String,
    members: List<TeamMember>,
    onMemberClick: (TeamMember) -> Unit
) {
    val trimmedQuery = query.trim()

    if (trimmedQuery.length < 2) {
        return
    }

    val filteredMembers = members
        .filter { it.name.contains(trimmedQuery, ignoreCase = true) }
        .take(5)

    if (filteredMembers.isEmpty()) {
        return
    }

    Text(
        text = "Vorschläge",
        modifier = Modifier.fieldTopPadding(12),
        style = MaterialTheme.typography.titleSmall
    )
    filteredMembers.forEach { member ->
        OutlinedButton(
            onClick = { onMemberClick(member) },
            modifier = defaultActionModifier(topPadding = 8)
        ) {
            Text(text = member.name)
        }
    }
}
