package de.teamplaner.domain

import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.DutyType
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamEventType
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRole
import de.teamplaner.model.Teilnahme
import de.teamplaner.model.TeilnahmeStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DutyAssignmentServiceTest {
    private val members = listOf(
        TeamMember(id = "member-1", name = "Daniele V.", role = TeamRole.Trainer),
        TeamMember(id = "member-2", name = "Max Mustermann", role = TeamRole.Member),
        TeamMember(id = "member-3", name = "Sara Beispiel", role = TeamRole.Member)
    )
    private val team = Team(
        id = "team-1",
        name = "A-Jugend",
        inviteCode = "AJ2026",
        inviteCodeActive = true,
        members = members
    )
    private val duties = listOf(
        Duty(id = "duty-1", teamId = team.id, type = DutyType.Jersey, title = "Trikotdienst", description = ""),
        Duty(id = "duty-2", teamId = team.id, type = DutyType.Drinks, title = "Getränkedienst", description = ""),
        Duty(id = "duty-3", teamId = team.id, type = DutyType.Cleanup, title = "Aufräumen", description = "")
    )

    @Test
    fun createFairAssignmentsKeepsExistingAssignments() {
        val event = event(id = "event-1", dutyIds = duties.map { it.id })
        val existing = DutyAssignment(
            id = "assignment-existing",
            eventId = event.id,
            dutyId = "duty-1",
            memberId = "member-1"
        )

        val result = DutyAssignmentService().createFairAssignments(
            team = team,
            events = listOf(event),
            duties = duties,
            currentAssignments = listOf(existing),
            replaceExisting = false
        )

        assertTrue(result.contains(existing))
        assertEquals(3, result.size)
        assertEquals(duties.map { it.id }.toSet(), result.map { it.dutyId }.toSet())
    }

    @Test
    fun createFairAssignmentsDistributesDutiesAcrossMembers() {
        val event = event(id = "event-1", dutyIds = duties.map { it.id })

        val result = DutyAssignmentService().createFairAssignments(
            team = team,
            events = listOf(event),
            duties = duties,
            currentAssignments = emptyList(),
            replaceExisting = true
        )

        assertEquals(3, result.size)
        assertEquals(3, result.map { it.memberId }.toSet().size)
    }

    private fun event(id: String, dutyIds: List<String>): TeamEvent {
        return TeamEvent(
            id = id,
            teamId = team.id,
            type = TeamEventType.Training,
            title = "Training",
            date = "25.06.2026",
            time = "18:00",
            location = "Sportplatz",
            dutyIds = dutyIds,
            teilnahmen = members.map {
                Teilnahme(memberId = it.id, status = TeilnahmeStatus.Offen)
            }
        )
    }
}
