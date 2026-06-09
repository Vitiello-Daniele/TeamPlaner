package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
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
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamEventType
import de.teamplaner.model.TeamMember
import de.teamplaner.model.Teilnahme
import de.teamplaner.model.TeilnahmeStatus

@Composable
fun EventScreen(
    team: Team?,
    currentMember: TeamMember?,
    canManageEvents: Boolean,
    events: List<TeamEvent>,
    onEventCreate: (TeamEvent) -> Unit,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
    modifier: Modifier = Modifier
) {
    var editedEvent by remember { mutableStateOf<TeamEvent?>(null) }

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
        } else {
            if (canManageEvents) {
                EventForm(
                    team = team,
                    editedEvent = editedEvent,
                    onEventSave = { event ->
                        val currentEditedEvent = editedEvent

                        if (currentEditedEvent == null) {
                            onEventCreate(event)
                        } else {
                            onEventUpdate(currentEditedEvent, event)
                            editedEvent = null
                        }
                    },
                    onCancelEdit = { editedEvent = null }
                )
            }
            EventList(
                events = events,
                currentMember = currentMember,
                canManageEvents = canManageEvents,
                onEventUpdate = onEventUpdate,
                onEventEdit = { event -> editedEvent = event },
                onEventRemove = onEventRemove
            )
        }
    }
}

@Composable
private fun EventForm(
    team: Team,
    editedEvent: TeamEvent?,
    onEventSave: (TeamEvent) -> Unit,
    onCancelEdit: () -> Unit
) {
    var title by remember(editedEvent) { mutableStateOf(editedEvent?.title.orEmpty()) }
    var date by remember(editedEvent) { mutableStateOf(editedEvent?.date.orEmpty()) }
    var time by remember(editedEvent) { mutableStateOf(editedEvent?.time.orEmpty()) }
    var location by remember(editedEvent) { mutableStateOf(editedEvent?.location.orEmpty()) }
    var eventType by remember(editedEvent) {
        mutableStateOf(editedEvent?.type ?: TeamEventType.Training)
    }
    var selectedMembers by remember(team, editedEvent) {
        mutableStateOf(editedEvent?.teilnahmen?.map { it.member }?.toSet() ?: team.members.toSet())
    }

    Text(
        text = if (editedEvent == null) "Neuer Termin" else "Termin bearbeiten",
        modifier = Modifier.fieldTopPadding(32),
        style = MaterialTheme.typography.titleMedium
    )

    AuthTextField(
        value = title,
        onValueChange = { title = it },
        label = "Titel",
        modifier = Modifier.fieldTopPadding(12)
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

            if (trimmedTitle.isNotBlank()) {
                onEventSave(
                    TeamEvent(
                        type = eventType,
                        title = trimmedTitle,
                        date = date.trim(),
                        time = time.trim(),
                        location = location.trim(),
                        teilnahmen = selectedMembers.map { member ->
                            editedEvent?.teilnahmen?.firstOrNull { it.member == member }
                                ?: Teilnahme(
                                    member = member,
                                    status = TeilnahmeStatus.Offen
                                )
                        }
                    )
                )
                title = ""
                date = ""
                time = ""
                location = ""
                eventType = TeamEventType.Training
                selectedMembers = team.members.toSet()
            }
        },
        modifier = defaultActionModifier(topPadding = 24)
    ) {
        Text(text = if (editedEvent == null) "Termin speichern" else "Aenderungen speichern")
    }

    if (editedEvent != null) {
        OutlinedButton(
            onClick = onCancelEdit,
            modifier = defaultActionModifier(topPadding = 12)
        ) {
            Text(text = "Abbrechen")
        }
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
private fun EventList(
    events: List<TeamEvent>,
    currentMember: TeamMember?,
    canManageEvents: Boolean,
    onEventUpdate: (TeamEvent, TeamEvent) -> Unit,
    onEventEdit: (TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit
) {
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
                currentMember = currentMember,
                canManageEvents = canManageEvents,
                onStatusChange = { teilnahme, status ->
                    onEventUpdate(
                        event,
                        event.copy(
                            teilnahmen = event.teilnahmen.map {
                                if (it == teilnahme) {
                                    it.copy(status = status)
                                } else {
                                    it
                                }
                            }
                        )
                    )
                },
                onEditClick = { onEventEdit(event) },
                onRemoveClick = { onEventRemove(event) }
            )
        }
    }
}

@Composable
private fun EventListItem(
    event: TeamEvent,
    currentMember: TeamMember?,
    canManageEvents: Boolean,
    onStatusChange: (Teilnahme, TeilnahmeStatus) -> Unit,
    onEditClick: () -> Unit,
    onRemoveClick: () -> Unit
) {
    var showDetails by remember { mutableStateOf(false) }
    val offene = event.teilnahmen.count { it.status == TeilnahmeStatus.Offen }
    val zugesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Zugesagt }
    val abgesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Abgesagt }

    Column(
        modifier = defaultActionModifier(topPadding = 16)
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
            text = "${event.date} ${event.time}",
            modifier = Modifier.fieldTopPadding(4),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = event.location.ifBlank { "Kein Ort angegeben" },
            modifier = Modifier.fieldTopPadding(4),
            style = MaterialTheme.typography.bodyMedium
        )
        Text(
            text = "Status: $zugesagt zugesagt, $abgesagt abgesagt, $offene offen",
            modifier = Modifier.fieldTopPadding(4),
            style = MaterialTheme.typography.bodyMedium
        )
        Button(
            onClick = { showDetails = !showDetails },
            modifier = defaultActionModifier(topPadding = 8)
        ) {
            Text(text = if (showDetails) "Details ausblenden" else "Details")
        }
        if (showDetails) {
            event.teilnahmen.forEach { teilnahme ->
                TeilnahmeRow(
                    teilnahme = teilnahme,
                    canEditStatus = canManageEvents || teilnahme.member == currentMember,
                    onStatusChange = { status -> onStatusChange(teilnahme, status) }
                )
            }
            if (canManageEvents) {
                Button(
                    onClick = onEditClick,
                    modifier = defaultActionModifier(topPadding = 8)
                ) {
                    Text(text = "Bearbeiten")
                }
                OutlinedButton(
                    onClick = onRemoveClick,
                    modifier = defaultActionModifier(topPadding = 8)
                ) {
                    Text(text = "Termin loeschen")
                }
            }
        }
    }
}

@Composable
private fun TeilnahmeRow(
    teilnahme: Teilnahme,
    canEditStatus: Boolean,
    onStatusChange: (TeilnahmeStatus) -> Unit
) {
    Text(
        text = "${teilnahme.member.name}: ${teilnahme.status.label}",
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
