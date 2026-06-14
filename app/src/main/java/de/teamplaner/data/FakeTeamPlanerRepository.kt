package de.teamplaner.data

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

        val currentUser = TeamMember(
            name = displayName,
            role = TeamRole.Trainer
        )
        val member = TeamMember(
            name = "Max",
            role = TeamRole.Member
        )
        val team = Team(
            name = "Demo Team",
            inviteCode = "DEMO01",
            inviteCodeActive = true,
            members = listOf(currentUser, member)
        )
        val event = TeamEvent(
            type = TeamEventType.Training,
            title = "Training",
            date = "12.06.2026",
            time = "18:00",
            location = "Sportplatz",
            teilnahmen = listOf(
                Teilnahme(
                    member = currentUser,
                    status = TeilnahmeStatus.Zugesagt
                ),
                Teilnahme(
                    member = member,
                    status = TeilnahmeStatus.Offen
                )
            )
        )

        return TeamPlanerData(
            team = team,
            events = listOf(event)
        )
    }

    override fun saveData(data: TeamPlanerData) {
        savedData = data
    }
}
