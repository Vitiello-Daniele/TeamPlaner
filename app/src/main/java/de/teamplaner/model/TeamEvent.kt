package de.teamplaner.model

data class TeamEvent(
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val participants: List<TeamMember>
)
