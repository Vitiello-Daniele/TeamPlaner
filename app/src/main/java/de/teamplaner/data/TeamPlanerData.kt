package de.teamplaner.data

import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent

data class TeamPlanerData(
    val team: Team?,
    val events: List<TeamEvent>
)
