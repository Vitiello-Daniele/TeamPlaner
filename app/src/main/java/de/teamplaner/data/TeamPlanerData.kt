package de.teamplaner.data

import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamRequest

data class TeamPlanerData(
    val teams: List<Team>,
    val events: List<TeamEvent>,
    val duties: List<Duty>,
    val assignments: List<DutyAssignment>,
    val requests: List<TeamRequest> = emptyList()
)
