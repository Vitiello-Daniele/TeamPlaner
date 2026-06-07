package de.teamplaner.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.Checkbox
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
import de.teamplaner.model.TeamMember

@Composable
fun EventScreen(
    team: Team?,
    events: List<TeamEvent>,
    onEventCreate: (TeamEvent) -> Unit,
    onEventRemove: (TeamEvent) -> Unit,
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
        } else {
            EventForm(
                team = team,
                onEventCreate = onEventCreate
            )
            EventList(
                events = events,
                onEventRemove = onEventRemove
            )
        }
    }
}

@Composable
private fun EventForm(
    team: Team,
    onEventCreate: (TeamEvent) -> Unit
) {
    var title by remember { mutableStateOf("") }
    var date by remember { mutableStateOf("") }
    var time by remember { mutableStateOf("") }
    var location by remember { mutableStateOf("") }
    var selectedMembers by remember(team) { mutableStateOf(team.members.toSet()) }

    AuthTextField(
        value = title,
        onValueChange = { title = it },
        label = "Titel",
        modifier = Modifier.fieldTopPadding(32)
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
                onEventCreate(
                    TeamEvent(
                        title = trimmedTitle,
                        date = date.trim(),
                        time = time.trim(),
                        location = location.trim(),
                        participants = selectedMembers.toList()
                    )
                )
                title = ""
                date = ""
                time = ""
                location = ""
                selectedMembers = team.members.toSet()
            }
        },
        modifier = defaultActionModifier(topPadding = 24)
    ) {
        Text(text = "Termin speichern")
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
                onRemoveClick = { onEventRemove(event) }
            )
        }
    }
}

@Composable
private fun EventListItem(
    event: TeamEvent,
    onRemoveClick: () -> Unit
) {
    Column(
        modifier = defaultActionModifier(topPadding = 16)
    ) {
        Text(
            text = event.title,
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
            text = "Teilnehmer: ${event.participants.joinToString { it.name }}",
            modifier = Modifier.fieldTopPadding(4),
            style = MaterialTheme.typography.bodyMedium
        )
        OutlinedButton(
            onClick = onRemoveClick,
            modifier = defaultActionModifier(topPadding = 8)
        ) {
            Text(text = "Termin loeschen")
        }
    }
}
