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
import de.teamplaner.model.TeamRequest
import de.teamplaner.model.TeamRequestStatus
import de.teamplaner.model.TeamRequestType
import de.teamplaner.model.TeamRole

class MainAppState(
    private val displayName: String,
    private val repository: TeamPlanerRepository = FakeTeamPlanerRepository(),
    initialData: TeamPlanerData = repository.loadData(displayName),
    private val inviteCodeGenerator: InviteCodeGenerator = InviteCodeGenerator(),
    private val idGenerator: IdGenerator = IdGenerator(),
    private val dutyAssignmentService: DutyAssignmentService = DutyAssignmentService()
) {
    private var allTeams by mutableStateOf(initialData.teams)

    val teams: List<Team>
        get() = allTeams.filter { team ->
            team.members.any { it.name == displayName }
        }

    var selectedTeamId by mutableStateOf(teams.firstOrNull()?.id)
        private set

    var events by mutableStateOf(initialData.events)
        private set

    var duties by mutableStateOf(initialData.duties)
        private set

    var assignments by mutableStateOf(initialData.assignments)
        private set

    var requests by mutableStateOf(initialData.requests)
        private set

    private var inviteCodeNumber = 1

    val selectedTeam: Team?
        get() = teams.firstOrNull { it.id == selectedTeamId }

    val currentMember: TeamMember?
        get() = selectedTeam?.members?.firstOrNull { it.name == displayName }

    val isTrainer: Boolean
        get() = currentMember?.role == TeamRole.Trainer

    val trainerTeams: List<Team>
        get() = teams.filter(::canManageTeam)

    val openInvites: List<TeamRequest>
        get() = requests.filter {
            it.userName == displayName &&
                it.type == TeamRequestType.Invite &&
                it.status == TeamRequestStatus.Open
        }

    val openJoinRequests: List<TeamRequest>
        get() = requests.filter {
            it.userName == displayName &&
                it.type == TeamRequestType.JoinRequest &&
                it.status == TeamRequestStatus.Open
        }

    fun selectTeam(teamId: String) {
        selectedTeamId = teamId
    }

    fun memberForTeam(team: Team): TeamMember? {
        return team.members.firstOrNull { it.name == displayName }
    }

    fun canManageTeam(team: Team): Boolean {
        return memberForTeam(team)?.role == TeamRole.Trainer
    }

    fun teamEvents(teamId: String): List<TeamEvent> {
        return events.filter { it.teamId == teamId }
    }

    fun teamDuties(teamId: String): List<Duty> {
        return duties.filter { it.teamId == teamId }
    }

    fun teamAssignments(teamId: String): List<DutyAssignment> {
        val eventIds = teamEvents(teamId).map { it.id }.toSet()
        return assignments.filter { it.eventId in eventIds }
    }

    fun createTeam(teamName: String) {
        val teamId = idGenerator.create("team")
        val team = Team(
            id = teamId,
            name = teamName,
            inviteCode = inviteCodeGenerator.create(teamName, inviteCodeNumber),
            inviteCodeActive = true,
            members = listOf(
                TeamMember(
                    id = idGenerator.create("member"),
                    name = displayName,
                    role = TeamRole.Trainer
                )
            )
        )

        allTeams = allTeams + team
        selectedTeamId = teamId
        saveData()
    }

    fun joinTeam(inviteCode: String) {
        val trimmedCode = inviteCode.trim()
        val existingTeam = allTeams.firstOrNull {
            it.inviteCodeActive && it.inviteCode.equals(trimmedCode, ignoreCase = true)
        } ?: return

        if (existingTeam.members.any { it.name == displayName }) {
            selectedTeamId = existingTeam.id
            return
        }

        val alreadyOpen = requests.any {
            it.teamId == existingTeam.id &&
                it.userName == displayName &&
                it.type == TeamRequestType.JoinRequest &&
                it.status == TeamRequestStatus.Open
        }

        if (!alreadyOpen) {
            requests = requests + TeamRequest(
                id = idGenerator.create("request"),
                teamId = existingTeam.id,
                userName = displayName,
                type = TeamRequestType.JoinRequest,
                status = TeamRequestStatus.Open
            )
        }
        saveData()
    }

    fun inviteMember(memberName: String) {
        val currentTeam = selectedTeam
        val trimmedName = memberName.trim()

        if (
            currentTeam != null &&
            trimmedName.isNotBlank() &&
            currentTeam.members.none { it.name == trimmedName } &&
            requests.none {
                it.teamId == currentTeam.id &&
                    it.userName == trimmedName &&
                    it.type == TeamRequestType.Invite &&
                    it.status == TeamRequestStatus.Open
            }
        ) {
            requests = requests + TeamRequest(
                id = idGenerator.create("request"),
                teamId = currentTeam.id,
                userName = trimmedName,
                type = TeamRequestType.Invite,
                status = TeamRequestStatus.Open
            )
            saveData()
        }
    }

    fun openJoinRequests(teamId: String): List<TeamRequest> {
        return requests.filter {
            it.teamId == teamId &&
                it.type == TeamRequestType.JoinRequest &&
                it.status == TeamRequestStatus.Open
        }
    }

    fun openInvites(teamId: String): List<TeamRequest> {
        return requests.filter {
            it.teamId == teamId &&
                it.type == TeamRequestType.Invite &&
                it.status == TeamRequestStatus.Open
        }
    }

    fun teamName(teamId: String): String {
        return allTeams.firstOrNull { it.id == teamId }?.name ?: "Unbekanntes Team"
    }

    fun acceptRequest(request: TeamRequest) {
        val team = allTeams.firstOrNull { it.id == request.teamId } ?: return
        val alreadyMember = team.members.any { it.name == request.userName }

        if (!alreadyMember) {
            replaceTeam(
                team.copy(
                    members = team.members + TeamMember(
                        id = idGenerator.create("member"),
                        name = request.userName,
                        role = TeamRole.Member
                    )
                )
            )
        }

        updateRequestStatus(request, TeamRequestStatus.Accepted)
        selectedTeamId = request.teamId
        saveData()
    }

    fun rejectRequest(request: TeamRequest) {
        updateRequestStatus(request, TeamRequestStatus.Rejected)
        saveData()
    }

    fun removeMember(member: TeamMember) {
        val currentTeam = selectedTeam

        if (currentTeam != null && member.role != TeamRole.Trainer) {
            replaceTeam(
                currentTeam.copy(
                    members = currentTeam.members.filterNot { it.id == member.id }
                )
            )
            events = events.map { event ->
                event.copy(
                    teilnahmen = event.teilnahmen.filterNot {
                        it.memberId == member.id
                    }
                )
            }
            assignments = assignments.filterNot { it.memberId == member.id }
            saveData()
        }
    }

    fun refreshInviteCode() {
        val currentTeam = selectedTeam

        if (currentTeam != null) {
            val nextNumber = inviteCodeNumber + 1
            inviteCodeNumber = nextNumber
            replaceTeam(
                currentTeam.copy(
                    inviteCode = inviteCodeGenerator.create(currentTeam.name, nextNumber),
                    inviteCodeActive = true
                )
            )
            saveData()
        }
    }

    fun deactivateInviteCode() {
        val currentTeam = selectedTeam

        if (currentTeam != null) {
            replaceTeam(currentTeam.copy(inviteCodeActive = false))
            saveData()
        }
    }

    fun createEvent(event: TeamEvent) {
        val teamId = selectedTeamId ?: return
        events = events + event.copy(
            id = event.id.ifBlank { idGenerator.create("event") },
            teamId = teamId
        )
        saveData()
    }

    fun updateEvent(oldEvent: TeamEvent, newEvent: TeamEvent) {
        val eventIndex = events.indexOfFirst { it.id == oldEvent.id }

        if (eventIndex >= 0) {
            val updatedEvent = newEvent.copy(
                id = oldEvent.id,
                teamId = oldEvent.teamId
            )

            events = events.toMutableList().also {
                it[eventIndex] = updatedEvent
            }
            assignments = assignments.filterNot { assignment ->
                assignment.eventId == updatedEvent.id && assignment.dutyId !in updatedEvent.dutyIds
            }
            saveData()
        }
    }

    fun removeEvent(event: TeamEvent) {
        events = events.filterNot { it.id == event.id }
        assignments = assignments.filterNot { it.eventId == event.id }
        saveData()
    }

    fun createDuty(duty: Duty) {
        val teamId = selectedTeamId ?: return
        duties = duties + duty.copy(
            id = duty.id.ifBlank { idGenerator.create("duty") },
            teamId = teamId
        )
        saveData()
    }

    fun removeDuty(duty: Duty) {
        duties = duties.filterNot { it.id == duty.id }
        events = events.map { event ->
            event.copy(dutyIds = event.dutyIds.filterNot { it == duty.id })
        }
        assignments = assignments.filterNot { it.dutyId == duty.id }
        saveData()
    }

    fun assignDuty(event: TeamEvent, duty: Duty, member: TeamMember) {
        assignments = assignments
            .filterNot { it.eventId == event.id && it.dutyId == duty.id }
            .plus(
                DutyAssignment(
                    id = idGenerator.create("assignment"),
                    eventId = event.id,
                    dutyId = duty.id,
                    memberId = member.id
                )
            )
        saveData()
    }

    fun removeAssignment(assignment: DutyAssignment) {
        assignments = assignments.filterNot { it.id == assignment.id }
        saveData()
    }

    fun createFairPlan(replaceExisting: Boolean) {
        val currentTeam = selectedTeam ?: return
        val currentEvents = teamEvents(currentTeam.id)
        val currentDuties = teamDuties(currentTeam.id)
        val currentEventIds = currentEvents.map { it.id }.toSet()
        val otherAssignments = assignments.filterNot { it.eventId in currentEventIds }
        val currentAssignments = teamAssignments(currentTeam.id)

        if (currentEvents.isNotEmpty() && currentDuties.isNotEmpty()) {
            assignments = otherAssignments + dutyAssignmentService.createFairAssignments(
                team = currentTeam,
                events = currentEvents,
                duties = currentDuties,
                currentAssignments = currentAssignments,
                replaceExisting = replaceExisting
            )
            saveData()
        }
    }

    private fun replaceTeam(team: Team) {
        allTeams = allTeams.map {
            if (it.id == team.id) team else it
        }
    }

    private fun saveData() {
        repository.saveData(
            TeamPlanerData(
                teams = allTeams,
                events = events,
                duties = duties,
                assignments = assignments,
                requests = requests
            )
        )
    }

    private fun updateRequestStatus(
        request: TeamRequest,
        status: TeamRequestStatus
    ) {
        requests = requests.map {
            if (it.id == request.id) {
                it.copy(status = status)
            } else {
                it
            }
        }
    }
}
