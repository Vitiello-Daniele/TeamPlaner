package de.teamplaner.data

import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent

data class TeamPlanerData(
    val team: Team?,
    val events: List<TeamEvent>,
    val duties: List<Duty>,
    val assignments: List<DutyAssignment>
)
