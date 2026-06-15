package de.teamplaner.ui.state

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import de.teamplaner.data.FakeTeamPlanerRepository
import de.teamplaner.data.TeamPlanerData
import de.teamplaner.data.TeamPlanerRepository
import de.teamplaner.domain.DutyAssignmentService
import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRole

class MainAppState(
    private val displayName: String,
    private val repository: TeamPlanerRepository = FakeTeamPlanerRepository(),
    private val inviteCodeGenerator: InviteCodeGenerator = InviteCodeGenerator(),
    private val dutyAssignmentService: DutyAssignmentService = DutyAssignmentService()
) {
    private val initialData = repository.loadData(displayName)

    var team by mutableStateOf(initialData.team)
        private set

    var events by mutableStateOf(initialData.events)
        private set

    var duties by mutableStateOf(initialData.duties)
        private set

    var assignments by mutableStateOf(initialData.assignments)
        private set

    private var inviteCodeNumber = 1

    val currentMember: TeamMember?
        get() = team?.members?.firstOrNull { it.name == displayName }

    val isTrainer: Boolean
        get() = currentMember?.role == TeamRole.Trainer

    fun createTeam(teamName: String) {
        team = Team(
            name = teamName,
            inviteCode = inviteCodeGenerator.create(teamName, inviteCodeNumber),
            inviteCodeActive = true,
            members = listOf(
                TeamMember(
                    name = displayName,
                    role = TeamRole.Trainer
                )
            )
        )
        saveData()
    }

    fun joinTeam(inviteCode: String) {
        team = Team(
            name = "Team $inviteCode",
            inviteCode = inviteCode,
            inviteCodeActive = true,
            members = listOf(
                TeamMember(
                    name = displayName,
                    role = TeamRole.Member
                )
            )
        )
        saveData()
    }

    fun addMember(memberName: String) {
        val currentTeam = team
        val trimmedName = memberName.trim()

        if (
            currentTeam != null &&
            trimmedName.isNotBlank() &&
            currentTeam.members.none { it.name == trimmedName }
        ) {
            team = currentTeam.copy(
                members = currentTeam.members + TeamMember(
                    name = trimmedName,
                    role = TeamRole.Member
                )
            )
            saveData()
        }
    }

    fun removeMember(member: TeamMember) {
        val currentTeam = team

        if (currentTeam != null && member.role != TeamRole.Trainer) {
            team = currentTeam.copy(
                members = currentTeam.members - member
            )
            events = events.map { event ->
                event.copy(
                    teilnahmen = event.teilnahmen.filterNot {
                        it.member == member
                    }
                )
            }
            assignments = assignments.filterNot { it.member == member }
            saveData()
        }
    }

    fun refreshInviteCode() {
        val currentTeam = team

        if (currentTeam != null) {
            val nextNumber = inviteCodeNumber + 1
            inviteCodeNumber = nextNumber
            team = currentTeam.copy(
                inviteCode = inviteCodeGenerator.create(currentTeam.name, nextNumber),
                inviteCodeActive = true
            )
            saveData()
        }
    }

    fun deactivateInviteCode() {
        val currentTeam = team

        if (currentTeam != null) {
            team = currentTeam.copy(inviteCodeActive = false)
            saveData()
        }
    }

    fun createEvent(event: TeamEvent) {
        events = events + event
        saveData()
    }

    fun updateEvent(oldEvent: TeamEvent, newEvent: TeamEvent) {
        val eventIndex = events.indexOf(oldEvent)

        if (eventIndex >= 0) {
            events = events.toMutableList().also {
                it[eventIndex] = newEvent
            }
            saveData()
        }
    }

    fun removeEvent(event: TeamEvent) {
        events = events - event
        assignments = assignments.filterNot { it.event == event }
        saveData()
    }

    fun createDuty(duty: Duty) {
        duties = duties + duty
        saveData()
    }

    fun removeDuty(duty: Duty) {
        duties = duties - duty
        assignments = assignments.filterNot { it.duty == duty }
        saveData()
    }

    fun assignDuty(event: TeamEvent, duty: Duty, member: TeamMember) {
        assignments = assignments
            .filterNot { it.event == event && it.duty == duty }
            .plus(
                DutyAssignment(
                    event = event,
                    duty = duty,
                    member = member
                )
            )
        saveData()
    }

    fun removeAssignment(assignment: DutyAssignment) {
        assignments = assignments - assignment
        saveData()
    }

    fun createFairPlan(replaceExisting: Boolean) {
        val currentTeam = team

        if (currentTeam != null && events.isNotEmpty() && duties.isNotEmpty()) {
            assignments = dutyAssignmentService.createFairAssignments(
                team = currentTeam,
                events = events,
                duties = duties,
                currentAssignments = assignments,
                replaceExisting = replaceExisting
            )
            saveData()
        }
    }

    private fun saveData() {
        repository.saveData(
            TeamPlanerData(
                team = team,
                events = events,
                duties = duties,
                assignments = assignments
            )
        )
    }
}
