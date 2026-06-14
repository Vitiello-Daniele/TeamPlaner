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
import de.teamplaner.model.DutyType
import de.teamplaner.model.Team

private sealed interface DutyView {
    data object List : DutyView
    data object Form : DutyView
}

@Composable
fun DutyScreen(
    team: Team?,
    canManageDuties: Boolean,
    duties: List<Duty>,
    onDutyCreate: (Duty) -> Unit,
    onDutyRemove: (Duty) -> Unit,
    modifier: Modifier = Modifier
) {
    var dutyView by remember { mutableStateOf<DutyView>(DutyView.List) }

    when (dutyView) {
        DutyView.List -> DutyListScreen(
            team = team,
            canManageDuties = canManageDuties,
            duties = duties,
            onCreateClick = { dutyView = DutyView.Form },
            onDutyRemove = onDutyRemove,
            modifier = modifier
        )
        DutyView.Form -> DutyFormScreen(
            onBackClick = { dutyView = DutyView.List },
            onDutyCreate = { duty ->
                onDutyCreate(duty)
                dutyView = DutyView.List
            },
            modifier = modifier
        )
    }
}

@Composable
private fun DutyListScreen(
    team: Team?,
    canManageDuties: Boolean,
    duties: List<Duty>,
    onCreateClick: () -> Unit,
    onDutyRemove: (Duty) -> Unit,
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
            text = "Dienste",
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

        if (canManageDuties) {
            Button(
                onClick = onCreateClick,
                modifier = defaultActionModifier(topPadding = 24)
            ) {
                Text(text = "Dienst anlegen")
            }
        }

        if (duties.isEmpty()) {
            Text(
                text = "Noch keine Dienste angelegt",
                modifier = Modifier.fieldTopPadding(24),
                style = MaterialTheme.typography.bodyLarge
            )
        } else {
            duties.forEach { duty ->
                DutyListItem(
                    duty = duty,
                    canManageDuties = canManageDuties,
                    onRemoveClick = { onDutyRemove(duty) }
                )
            }
        }
    }
}

@Composable
private fun DutyListItem(
    duty: Duty,
    canManageDuties: Boolean,
    onRemoveClick: () -> Unit
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
                text = duty.title,
                style = MaterialTheme.typography.titleMedium
            )
            Text(
                text = duty.description.ifBlank { "Keine Beschreibung" },
                modifier = Modifier.fieldTopPadding(8),
                style = MaterialTheme.typography.bodyMedium
            )
            if (canManageDuties) {
                OutlinedButton(
                    onClick = onRemoveClick,
                    modifier = defaultActionModifier(topPadding = 12)
                ) {
                    Text(text = "Dienst löschen")
                }
            }
        }
    }
}

@Composable
private fun DutyFormScreen(
    onBackClick: () -> Unit,
    onDutyCreate: (Duty) -> Unit,
    modifier: Modifier = Modifier
) {
    var title by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dutyType by remember { mutableStateOf(DutyType.Jersey) }
    var errorText by remember { mutableStateOf("") }

    Column(
        modifier = modifier
            .fillMaxSize()
            .verticalScroll(rememberScrollState())
            .padding(16.dp)
    ) {
        ScreenHeader(
            title = "Dienst anlegen",
            onBackClick = onBackClick
        )
        Text(
            text = "Diensttyp",
            modifier = Modifier.fieldTopPadding(24),
            style = MaterialTheme.typography.titleMedium
        )
        DutyType.entries.forEach { type ->
            FilterChip(
                selected = dutyType == type,
                onClick = {
                    dutyType = type
                    title = type.label
                    description = type.defaultDescription
                },
                label = { Text(text = type.label) },
                modifier = Modifier.fieldTopPadding(8)
            )
        }
        AuthTextField(
            value = title,
            onValueChange = { title = it },
            label = "Titel",
            modifier = Modifier.fieldTopPadding(24)
        )
        AuthTextField(
            value = description,
            onValueChange = { description = it },
            label = "Beschreibung",
            modifier = Modifier.fieldTopPadding(12)
        )
        Button(
            onClick = {
                val trimmedTitle = title.trim()

                if (trimmedTitle.isBlank()) {
                    errorText = "Bitte einen Titel eingeben"
                } else {
                    onDutyCreate(
                        Duty(
                            type = dutyType,
                            title = trimmedTitle,
                            description = description.trim()
                        )
                    )
                }
            },
            modifier = defaultActionModifier(topPadding = 24)
        ) {
            Text(text = "Speichern")
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
