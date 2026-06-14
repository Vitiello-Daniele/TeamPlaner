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
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
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
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.dp
import de.teamplaner.R
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
    var selectedEvent by remember { mutableStateOf<TeamEvent?>(null) }

    val openEvent = selectedEvent
    if (openEvent != null) {
        EventDetailScreen(
            event = openEvent,
            currentMember = currentMember,
            canManageEvents = canManageEvents,
            onBackClick = { selectedEvent = null },
            onEventUpdate = { oldEvent, newEvent ->
                onEventUpdate(oldEvent, newEvent)
                selectedEvent = newEvent
            },
            onEventEdit = {
                editedEvent = openEvent
                selectedEvent = null
            },
            onEventRemove = {
                onEventRemove(openEvent)
                selectedEvent = null
            },
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
                onEventRemove = onEventRemove,
                onEventOpen = { event -> selectedEvent = event }
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
    var errorText by remember(editedEvent) { mutableStateOf("") }

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
            val trimmedDate = date.trim()
            val trimmedTime = time.trim()

            if (trimmedTitle.isBlank()) {
                errorText = "Bitte einen Titel eingeben"
            } else if (trimmedDate.isBlank()) {
                errorText = "Bitte ein Datum eingeben"
            } else if (trimmedTime.isBlank()) {
                errorText = "Bitte eine Uhrzeit eingeben"
            } else if (selectedMembers.isEmpty()) {
                errorText = "Bitte mindestens einen Teilnehmer auswaehlen"
            } else {
                onEventSave(
                    TeamEvent(
                        type = eventType,
                        title = trimmedTitle,
                        date = trimmedDate,
                        time = trimmedTime,
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
                errorText = ""
            }
        },
        modifier = defaultActionModifier(topPadding = 24)
    ) {
        Text(text = if (editedEvent == null) "Termin speichern" else "Aenderungen speichern")
    }
    if (errorText.isNotBlank()) {
        Text(
            text = errorText,
            modifier = Modifier.fieldTopPadding(8),
            style = MaterialTheme.typography.bodyMedium
        )
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
    onEventRemove: (TeamEvent) -> Unit,
    onEventOpen: (TeamEvent) -> Unit
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
                onRemoveClick = { onEventRemove(event) },
                onOpenClick = { onEventOpen(event) }
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
    onRemoveClick: () -> Unit,
    onOpenClick: () -> Unit
) {
    val offene = event.teilnahmen.count { it.status == TeilnahmeStatus.Offen }
    val zugesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Zugesagt }
    val abgesagt = event.teilnahmen.count { it.status == TeilnahmeStatus.Abgesagt }

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
        }
    }
}

@Composable
private fun EventDetailScreen(
    event: TeamEvent,
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

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically
        ) {
            IconButton(onClick = onBackClick) {
                Icon(
                    painter = painterResource(id = R.drawable.ic_arrow_back),
                    contentDescription = "Zurück"
                )
            }
            Text(
                text = "Termindetails",
                style = MaterialTheme.typography.headlineSmall
            )
        }

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
            text = "Teilnehmer",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        event.teilnahmen.forEach { teilnahme ->
            TeilnahmeRow(
                teilnahme = teilnahme,
                canEditStatus = canManageEvents || teilnahme.member == currentMember,
                onStatusChange = { status ->
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
