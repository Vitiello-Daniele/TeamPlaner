package de.teamplaner.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.Checkbox
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
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamEventType
import de.teamplaner.model.TeamMember
import de.teamplaner.model.Teilnahme
import de.teamplaner.model.TeilnahmeStatus

private sealed interface EventView {
    data object List : EventView
    data class Form(val event: TeamEvent?) : EventView
    data class Detail(val event: TeamEvent) : EventView
}

@Composable
fun EventScreen(
    team: Team?,
    currentMember: TeamMember?,
    canManageEvents: Boolean,
    events: List<TeamEvent>,
    duties: List<Duty>,
    onEventCreate: (TeamEvent) -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
    onAutoAssign: () -> Unit,
    modifier: Modifier = Modifier
) {
    var eventView by remember { mutableStateOf<EventView>(EventView.List) }

    when (val currentView = eventView) {
        EventView.List -> EventListScreen(
            team = team,
            events = events,
            duties = duties,
            canManageEvents = canManageEvents,
            onCreateClick = { eventView = EventView.Form(event = null) },
            onEventOpen = { event -> eventView = EventView.Detail(event) },
            modifier = modifier
        )
        is EventView.Form -> {
            if (team == null) {
                eventView = EventView.List
            } else {
                EventFormScreen(
                    team = team,
                    duties = duties,
                    editedEvent = currentView.event,
                    onBackClick = { eventView = EventView.List },
                    onEventSave = { event, shouldAutoAssign ->
                        val editedEvent = currentView.event

                        if (editedEvent == null) {
                            onEventCreate(event)
                        } else {
                            onEventUpdate(editedEvent, event)
                        }
                        if (shouldAutoAssign) {
                            onAutoAssign()
                        }
                        eventView = EventView.List
                    },
                    modifier = modifier
                )
            }
        }
        is EventView.Detail -> EventDetailScreen(
            team = team,
            event = currentView.event,
            duties = duties,
            currentMember = currentMember,
            canManageEvents = canManageEvents,
            onBackClick = { eventView = EventView.List },
            onEventUpdate = { oldEvent, newEvent ->
                onEventUpdate(oldEvent, newEvent)
                eventView = EventView.Detail(newEvent)
            },
            onEventEdit = { eventView = EventView.Form(currentView.event) },
            onEventRemove = {
                onEventRemove(currentView.event)
                eventView = EventView.List
            },
            modifier = modifier
        )
    }
}

@Composable
private fun EventListScreen(
    team: Team?,
    events: List<TeamEvent>,
    duties: List<Duty>,
    canManageEvents: Boolean,
    onCreateClick: () -> Unit,
    onEventOpen: (TeamEvent) -> Unit,
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
            text = "Termine",
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

        if (canManageEvents) {
            Button(
                onClick = onCreateClick,
                modifier = defaultActionModifier(topPadding = 24)
            ) {
                Text(text = "Termin anlegen")
            }
        }

        Text(
            text = "Geplante Termine",
            modifier = Modifier.fieldTopPadding(32),
            style = MaterialTheme.typography.titleMedium
        )

        if (events.isEmpty()) {
            Text(
                text = "Noch keine Termine geplant",
                modifier = Modifier.fieldTopPadding(12),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            events.forEach { event ->
                EventListItem(
                    event = event,
                    duties = duties,
                    onOpenClick = { onEventOpen(event) }
                )
            }
        }
    }
}

@Composable
private fun EventFormScreen(
    team: Team,
    duties: List<Duty>,
    editedEvent: TeamEvent?,
    onBackClick: () -> Unit,
    onEventSave: (TeamEvent, Boolean) -> Unit,
    modifier: Modifier = Modifier
) {
    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = if (editedEvent == null) "Termin anlegen" else "Termin bearbeiten",
            onBackClick = onBackClick
        )
        EventForm(
            team = team,
            duties = duties,
            editedEvent = editedEvent,
            onEventSave = onEventSave
        )
    }
}

@Composable
private fun EventForm(
    team: Team,
    duties: List<Duty>,
    editedEvent: TeamEvent?,
    onEventSave: (TeamEvent, Boolean) -> Unit
) {
    var title by remember(editedEvent) { mutableStateOf(editedEvent?.title.orEmpty()) }
    var date by remember(editedEvent) { mutableStateOf(editedEvent?.date.orEmpty()) }
    var time by remember(editedEvent) { mutableStateOf(editedEvent?.time.orEmpty()) }
    var location by remember(editedEvent) { mutableStateOf(editedEvent?.location.orEmpty()) }
    var eventType by remember(editedEvent) {
        mutableStateOf(editedEvent?.type ?: TeamEventType.Training)
    }
    var selectedMembers by remember(team, editedEvent) {
        val selectedMemberIds = editedEvent?.teilnahmen?.map { it.memberId }?.toSet()
        mutableStateOf(
            if (selectedMemberIds == null) {
                team.members.toSet()
            } else {
                team.members.filter { it.id in selectedMemberIds }.toSet()
            }
        )
    }
    var selectedDutyIds by remember(duties, editedEvent) {
        mutableStateOf(editedEvent?.dutyIds?.toSet() ?: duties.map { it.id }.toSet())
    }
    var shouldAutoAssign by remember(editedEvent) { mutableStateOf(editedEvent == null) }
    var errorText by remember(editedEvent) { mutableStateOf("") }

    AuthTextField(
        value = title,
        onValueChange = { title = it },
        label = "Titel",
        modifier = Modifier.fieldTopPadding(24)
    )
    AuthTextField(
        value = date,
        onValueChange = { date = it },
        label = "Datum",
        modifier = Modifier.fieldTopPadding(12)
    )
    AuthTextField(
        value = time,
        onValueChange = { time = it },
        label = "Uhrzeit",
        modifier = Modifier.fieldTopPadding(12)
    )
    AuthTextField(
        value = location,
        onValueChange = { location = it },
        label = "Ort",
        modifier = Modifier.fieldTopPadding(12)
    )

    Text(
        text = "Art",
        modifier = Modifier.fieldTopPadding(24),
        style = MaterialTheme.typography.titleMedium
    )
    EventTypeSelection(
        selectedType = eventType,
        onTypeSelect = { eventType = it }
    )

    Text(
        text = "Benötigte Dienste",
        modifier = Modifier.fieldTopPadding(24),
        style = MaterialTheme.typography.titleMedium
    )
    if (duties.isEmpty()) {
        Text(
            text = "Für dieses Team sind noch keine Dienste angelegt.",
            modifier = Modifier.fieldTopPadding(8),
            style = MaterialTheme.typography.bodyMedium
        )
    } else {
        duties.forEach { duty ->
            DutySelectionRow(
                duty = duty,
                isSelected = selectedDutyIds.contains(duty.id),
                onSelectedChange = { isSelected ->
                    selectedDutyIds = if (isSelected) {
                        selectedDutyIds + duty.id
                    } else {
                        selectedDutyIds - duty.id
                    }
                }
            )
        }
        DutyAutoAssignRow(
            isSelected = shouldAutoAssign,
            onSelectedChange = { shouldAutoAssign = it }
        )
    }

    Text(
        text = "Teilnehmer",
        modifier = Modifier.fieldTopPadding(24),
        style = MaterialTheme.typography.titleMedium
    )
    team.members.forEach { member ->
        ParticipantRow(
            member = member,
            isSelected = selectedMembers.contains(member),
            onSelectedChange = { isSelected ->
                selectedMembers = if (isSelected) {
                    selectedMembers + member
                } else {
                    selectedMembers - member
                }
            }
        )
    }

    Button(
        onClick = {
            val trimmedTitle = title.trim()
            val trimmedDate = date.trim()
            val trimmedTime = time.trim()

            when {
                trimmedTitle.isBlank() -> errorText = "Bitte einen Titel eingeben"
                trimmedDate.isBlank() -> errorText = "Bitte ein Datum eingeben"
                trimmedTime.isBlank() -> errorText = "Bitte eine Uhrzeit eingeben"
                selectedMembers.isEmpty() -> errorText = "Bitte mindestens einen Teilnehmer auswählen"
                selectedDutyIds.isEmpty() -> errorText = "Bitte mindestens einen Dienst auswählen"
                else -> {
                    onEventSave(
                        TeamEvent(
                            id = editedEvent?.id.orEmpty(),
                            teamId = editedEvent?.teamId.orEmpty(),
                            type = eventType,
                            title = trimmedTitle,
                            date = trimmedDate,
                            time = trimmedTime,
                            location = location.trim(),
                            dutyIds = selectedDutyIds.toList(),
                            teilnahmen = selectedMembers.map { member ->
                                editedEvent?.teilnahmen?.firstOrNull { it.memberId == member.id }
                                    ?: Teilnahme(
                                        memberId = member.id,
                                        status = TeilnahmeStatus.Offen
                                    )
                            }
                        ),
                        shouldAutoAssign
                    )
                }
            }
        },
        modifier = defaultActionModifier(topPadding = 24)
    ) {
        Text(text = if (editedEvent == null) "Termin speichern" else "Änderungen speichern")
    }

    if (errorText.isNotBlank()) {
        Text(
            text = errorText,
            modifier = Modifier.fieldTopPadding(8),
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
private fun EventTypeSelection(
    selectedType: TeamEventType,
    onTypeSelect: (TeamEventType) -> Unit
) {
    Row(
        modifier = defaultActionModifier(topPadding = 8)
    ) {
        TeamEventType.entries.forEach { type ->
            FilterChip(
                selected = selectedType == type,
                onClick = { onTypeSelect(type) },
                label = { Text(text = type.label) },
                modifier = Modifier.padding(end = 8.dp)
            )
        }
    }
}

@Composable
private fun DutySelectionRow(
    duty: Duty,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row(
        modifier = defaultActionModifier(topPadding = 8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectedChange
        )
        Text(
            text = duty.title,
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun DutyAutoAssignRow(
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row(
        modifier = defaultActionModifier(topPadding = 8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectedChange
        )
        Text(
            text = "Nach dem Speichern automatisch zuweisen",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun ParticipantRow(
    member: TeamMember,
    isSelected: Boolean,
    onSelectedChange: (Boolean) -> Unit
) {
    Row(
        modifier = defaultActionModifier(topPadding = 8),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = isSelected,
            onCheckedChange = onSelectedChange
        )
        Text(
            text = "${member.name} (${member.role.label})",
            style = MaterialTheme.typography.bodyLarge
        )
    }
}

@Composable
private fun EventListItem(
    event: TeamEvent,
    duties: List<Duty>,
    onOpenClick: () -> Unit
) {
    val offene = event.teilnahmen.count { it.status == TeilnahmeStatus.Offen }
    val zugesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Zugesagt }
    val abgesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Abgesagt }
    val eventDuties = duties.filter { it.id in event.dutyIds }

    Card(
        modifier = Modifier
            .padding(top = 12.dp)
            .widthIn(max = 520.dp)
            .fillMaxWidth()
            .clickable(onClick = onOpenClick)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = event.type.label,
                style = MaterialTheme.typography.labelLarge
            )
            Text(
                text = event.title,
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = "${event.date} um ${event.time}",
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = event.location.ifBlank { "Kein Ort angegeben" },
                modifier = Modifier.fieldTopPadding(4),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = "$zugesagt zugesagt, $abgesagt abgesagt, $offene offen",
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
            Text(
                text = if (eventDuties.isEmpty()) {
                    "Keine Dienste ausgewählt"
                } else {
                    "Dienste: ${eventDuties.joinToString { it.title }}"
                },
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
        }
    }
}

@Composable
private fun EventDetailScreen(
    team: Team?,
    event: TeamEvent,
    duties: List<Duty>,
    currentMember: TeamMember?,
    canManageEvents: Boolean,
    onBackClick: () -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventEdit: () -> Unit,
    onEventRemove: () -> Unit,
    modifier: Modifier = Modifier
) {
    val offene = event.teilnahmen.count { it.status == TeilnahmeStatus.Offen }
    val zugesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Zugesagt }
    val abgesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Abgesagt }
    val eventDuties = duties.filter { it.id in event.dutyIds }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
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
            text = event.type.label,
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
            text = if (eventDuties.isEmpty()) {
                "Keine Dienste ausgewählt"
            } else {
                "Dienste: ${eventDuties.joinToString { it.title }}"
            },
            modifier = Modifier.fieldTopPadding(16),
            style = MaterialTheme.typography.bodyLarge
        )

        Text(
            text = "Teilnehmer",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        event.teilnahmen.forEach { teilnahme ->
            val member = team?.members?.firstOrNull { it.id == teilnahme.memberId }
            TeilnahmeRow(
                teilnahme = teilnahme,
                memberName = member?.name ?: "Unbekanntes Mitglied",
                canEditStatus = canManageEvents || teilnahme.memberId == currentMember?.id,
                onStatusChange = { status ->
                    onEventUpdate(
                        event,
                        event.copy(
                            teilnahmen = event.teilnahmen.map {
                                if (it.memberId == teilnahme.memberId) {
                                    it.copy(status = status)
                                } else {
                                    it
                                }
                            }
                        )
                    )
                }
            )
        }

        if (canManageEvents) {
            Button(
                onClick = onEventEdit,
                modifier = defaultActionModifier(topPadding = 24)
            ) {
                Text(text = "Bearbeiten")
            }
            OutlinedButton(
                onClick = onEventRemove,
                modifier = defaultActionModifier(topPadding = 8)
            ) {
                Text(text = "Termin löschen")
            }
        }
    }
}

@Composable
private fun TeilnahmeRow(
    teilnahme: Teilnahme,
    memberName: String,
    canEditStatus: Boolean,
    onStatusChange: (TeilnahmeStatus) -> Unit
) {
    Text(
        text = "$memberName: ${teilnahme.status.label}",
        modifier = Modifier.fieldTopPadding(8),
        style = MaterialTheme.typography.bodyMedium
    )
    if (canEditStatus) {
        Row(
            modifier = defaultActionModifier(topPadding = 4)
        ) {
            TeilnahmeStatus.entries.forEach { status ->
                FilterChip(
                    selected = teilnahme.status == status,
                    onClick = { onStatusChange(status) },
                    label = { Text(text = status.label) },
                    modifier = Modifier.padding(end = 8.dp)
                )
            }
        }
    }
}
