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
            .groupingBy { it.member }
            .eachCount()
            .toMutableMap()

        events.forEach { event ->
            duties.forEach { duty ->
                val alreadyAssigned = result.any { it.event == event && it.duty == duty }

                if (!alreadyAssigned) {
                    val member = findNextMember(
                        members = team.members,
                        event = event,
                        assignments = result,
                        assignmentCounts = assignmentCounts
                    )

                    if (member != null) {
                        result += DutyAssignment(
                            event = event,
                            duty = duty,
                            member = member
                        )
                        assignmentCounts[member] = (assignmentCounts[member] ?: 0) + 1
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
        assignmentCounts: Map<TeamMember, Int>
    ): TeamMember? {
        val membersAlreadyUsedForEvent = assignments
            .filter { it.event == event }
            .map { it.member }
            .toSet()
        val preferredMembers = members.filterNot { it in membersAlreadyUsedForEvent }
        val candidates = preferredMembers.ifEmpty { members }

        return candidates.minWithOrNull(
            compareBy<TeamMember> { assignmentCounts[it] ?: 0 }
                .thenBy { it.name }
        )
    }
}
