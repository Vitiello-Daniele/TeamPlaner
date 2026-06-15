package de.teamplaner.domain

import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember

class DutyAssignmentService {
    fun createFairAssignments(
        team: Team,
        events: List<TeamEvent>,
        duties: List<Duty>,
        currentAssignments: List<DutyAssignment>,
        replaceExisting: Boolean
    ): List<DutyAssignment> {
        val result = if (replaceExisting) {
            emptyList()
        } else {
            currentAssignments
        }.toMutableList()
        val assignmentCounts = result
            .groupingBy { it.memberId }
            .eachCount()
            .toMutableMap()

        events.forEach { event ->
            val eventDuties = duties.filter { it.id in event.dutyIds }

            eventDuties.forEach { duty ->
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
        val membersAlreadyUsedForEvent = assignments
            .filter { it.eventId == event.id }
            .map { it.memberId }
            .toSet()
        val preferredMembers = members.filterNot { it.id in membersAlreadyUsedForEvent }
        val candidates = preferredMembers.ifEmpty { members }

        return candidates.minWithOrNull(
            compareBy<TeamMember> { assignmentCounts[it.id] ?: 0 }
                .thenBy { it.name }
        )
    }
}
