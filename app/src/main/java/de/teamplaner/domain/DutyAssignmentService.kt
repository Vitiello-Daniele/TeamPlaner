package de.teamplaner.domain

import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember

// Verteilt die Dienste möglichst fair auf die Mitglieder.
// Idee: Wer bisher am wenigsten Dienste hatte, kommt als Nächstes dran.
class DutyAssignmentService {
    fun createFairAssignments(
        team: Team,
        events: List<TeamEvent>,
        duties: List<Duty>,
        currentAssignments: List<DutyAssignment>,
        replaceExisting: Boolean
    ): List<DutyAssignment> {
        // replaceExisting = true -> komplett neu verteilen,
        // false -> bestehende Zuweisungen behalten
        val result = if (replaceExisting) {
            emptyList()
        } else {
            currentAssignments
        }.toMutableList()
        // zählt pro Mitglied die bisherigen Dienste (Grundlage für "fair")
        val assignmentCounts = result
            .groupingBy { it.memberId }
            .eachCount()
            .toMutableMap()

        events.forEach { event ->
            val eventDuties = duties.filter { it.id in event.dutyIds }

            eventDuties.forEach { duty ->
                // schon vergebene Dienste nicht doppelt zuweisen
                val alreadyAssigned = result.any { it.eventId == event.id && it.dutyId == duty.id }

                if (!alreadyAssigned) {
                    val member = findNextMember(
                        members = team.members,
                        event = event,
                        assignments = result,
                        assignmentCounts = assignmentCounts
                    )

                    if (member != null) {
                        result += DutyAssignment(
                            id = "assignment-${event.id}-${duty.id}",
                            eventId = event.id,
                            dutyId = duty.id,
                            memberId = member.id
                        )
                        assignmentCounts[member.id] = (assignmentCounts[member.id] ?: 0) + 1
                    }
                }
            }
        }

        return result
    }

    private fun findNextMember(
        members: List<TeamMember>,
        event: TeamEvent,
        assignments: List<DutyAssignment>,
        assignmentCounts: Map<String, Int>
    ): TeamMember? {
        // möglichst niemand bekommt zwei Dienste im selben Termin
        val membersAlreadyUsedForEvent = assignments
            .filter { it.eventId == event.id }
            .map { it.memberId }
            .toSet()
        val preferredMembers = members.filterNot { it.id in membersAlreadyUsedForEvent }
        // falls schon alle einen Dienst haben, wieder alle zulassen
        val candidates = preferredMembers.ifEmpty { members }

        // Mitglied mit den wenigsten Diensten wählen,
        // bei Gleichstand alphabetisch (damit das Ergebnis stabil ist)
        return candidates.minWithOrNull(
            compareBy<TeamMember> { assignmentCounts[it.id] ?: 0 }
                .thenBy { it.name }
        )
    }
}
