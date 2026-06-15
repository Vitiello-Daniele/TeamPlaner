package de.teamplaner.data

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

class FakeTeamPlanerRepository : TeamPlanerRepository {
    private var savedData: TeamPlanerData? = null

    override fun loadData(displayName: String): TeamPlanerData {
        val currentData = savedData

        if (currentData != null) {
            return currentData
        }

        val daniele = TeamMember("member-daniele", TRAINER_NAME, TeamRole.Trainer)
        val leon = TeamMember("member-leon", "Leon M.", TeamRole.Member)
        val mainUser = when (displayName) {
            TRAINER_NAME -> daniele
            "Leon M." -> leon
            else -> TeamMember("member-${displayName.lowercase().replace(Regex("[^a-z0-9]+"), "-")}", displayName, TeamRole.Member)
        }
        val max = TeamMember("member-max", "Max K.", TeamRole.Member)
        val sara = TeamMember("member-sara", "Sara B.", TeamRole.Member)
        val mia = TeamMember("member-mia", "Mia S.", TeamRole.Member)
        val david = TeamMember("member-david", "David R.", TeamRole.Member)
        val teamA = Team(
            id = "team-a-jugend",
            name = "A-Jugend",
            inviteCode = "AJ2026",
            inviteCodeActive = true,
            members = listOf(daniele, leon, max, sara, mia, mainUser).distinctBy { it.id }
        )
        val teamB = Team(
            id = "team-herren-2",
            name = "Herren 2",
            inviteCode = "H22026",
            inviteCodeActive = true,
            members = listOf(daniele, leon, david, mainUser).distinctBy { it.id }
        )
        val duties = listOf(
            createDuty(teamA.id, DutyType.Jersey),
            createDuty(teamA.id, DutyType.Drinks),
            createDuty(teamA.id, DutyType.Cleanup),
            createDuty(teamB.id, DutyType.Drinks),
            createDuty(teamB.id, DutyType.Driving)
        )
        val events = listOf(
            TeamEvent(
                id = "event-training-a",
                teamId = teamA.id,
                type = TeamEventType.Training,
                title = "Training",
                date = "18.06.2026",
                time = "18:00",
                location = "Sportplatz Nord",
                dutyIds = duties.filter { it.teamId == teamA.id }.map { it.id },
                teilnahmen = teamA.members.map { member ->
                    Teilnahme(
                        memberId = member.id,
                        status = if (member.id == mainUser.id) TeilnahmeStatus.Zugesagt else TeilnahmeStatus.Offen
                    )
                }
            ),
            TeamEvent(
                id = "event-game-b",
                teamId = teamB.id,
                type = TeamEventType.Game,
                title = "Heimspiel",
                date = "21.06.2026",
                time = "15:00",
                location = "Hauptplatz",
                dutyIds = duties.filter { it.teamId == teamB.id }.map { it.id },
                teilnahmen = teamB.members.map { member ->
                    Teilnahme(
                        memberId = member.id,
                        status = if (member.id == mainUser.id) TeilnahmeStatus.Zugesagt else TeilnahmeStatus.Offen
                    )
                }
            )
        )
        val assignments = listOf(
            DutyAssignment(
                id = "assignment-a-jersey",
                eventId = events[0].id,
                dutyId = duties[0].id,
                memberId = max.id
            ),
            DutyAssignment(
                id = "assignment-b-drinks",
                eventId = events[1].id,
                dutyId = duties[3].id,
                memberId = mainUser.id
            )
        )

        return TeamPlanerData(
            teams = listOf(teamA, teamB),
            events = events,
            duties = duties,
            assignments = assignments
        )
    }

    override fun saveData(data: TeamPlanerData) {
        savedData = data
    }

    private fun createDuty(teamId: String, type: DutyType): Duty {
        return Duty(
            id = "duty-$teamId-${type.name.lowercase()}",
            teamId = teamId,
            type = type,
            title = type.label,
            description = type.defaultDescription
        )
    }

    private companion object {
        const val TRAINER_NAME = "Daniele V."
    }
}
