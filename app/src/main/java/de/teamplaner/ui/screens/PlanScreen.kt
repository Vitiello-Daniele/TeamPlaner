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

private sealed interface PlanView {
    data object List : PlanView
    data object Form : PlanView
}

@Composable
fun PlanScreen(
    team: Team?,
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

    when (planView) {
        PlanView.List -> PlanListScreen(
            team = team,
            events = events,
            duties = duties,
            assignments = assignments,
            canManageAssignments = canManageAssignments,
            onCreateClick = { planView = PlanView.Form },
            onFairPlanCreate = onFairPlanCreate,
            onAssignmentRemove = onAssignmentRemove,
            modifier = modifier
        )
        PlanView.Form -> AssignmentFormScreen(
            team = team,
            events = events,
            duties = duties,
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
    onCreateClick: () -> Unit,
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
                EventAssignmentCard(
                    event = event,
                    assignments = assignments.filter { it.event == event },
                    canManageAssignments = canManageAssignments,
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
        val count = assignments.count { it.member == member }

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
    assignments: List<DutyAssignment>,
    canManageAssignments: Boolean,
    onAssignmentRemove: (DutyAssignment) -> Unit
) {
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

            if (assignments.isEmpty()) {
                Text(
                    text = "Noch keine Dienste zugewiesen",
                    modifier = Modifier.fieldTopPadding(12),
                    style = MaterialTheme.typography.bodyMedium
                )
            } else {
                assignments.forEach { assignment ->
                    Text(
                        text = "${assignment.duty.title}: ${assignment.member.name}",
                        modifier = Modifier.fieldTopPadding(12),
                        style = MaterialTheme.typography.bodyLarge
                    )
                    if (canManageAssignments) {
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
private fun AssignmentFormScreen(
    team: Team?,
    events: List<TeamEvent>,
    duties: List<Duty>,
    onBackClick: () -> Unit,
    onDutyAssign: (TeamEvent, Duty, TeamMember) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedEvent by remember { mutableStateOf<TeamEvent?>(events.firstOrNull()) }
    var selectedDuty by remember { mutableStateOf<Duty?>(duties.firstOrNull()) }
    var memberQuery by remember { mutableStateOf("") }
    var selectedMember by remember { mutableStateOf<TeamMember?>(null) }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Dienst zuweisen",
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
        events.forEach { event ->
            FilterChip(
                selected = selectedEvent == event,
                onClick = { selectedEvent = event },
                label = { Text(text = event.title) },
                modifier = Modifier.fieldTopPadding(8)
            )
        }

        Text(
            text = "Dienst",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        duties.forEach { duty ->
            FilterChip(
                selected = selectedDuty == duty,
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

        Button(
            onClick = {
                val event = selectedEvent
                val duty = selectedDuty
                val member = selectedMember

                when {
                    events.isEmpty() -> errorText = "Bitte zuerst einen Termin anlegen"
                    duties.isEmpty() -> errorText = "Bitte zuerst einen Dienst anlegen"
                    event == null -> errorText = "Bitte einen Termin auswählen"
                    duty == null -> errorText = "Bitte einen Dienst auswählen"
                    member == null -> errorText = "Bitte ein Mitglied auswählen"
                    else -> onDutyAssign(event, duty, member)
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Zuweisen")
        }

        if (errorText.isNotBlank()) {
            Text(
                text = errorText,
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
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
