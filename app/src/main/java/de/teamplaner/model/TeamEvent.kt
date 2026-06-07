package de.teamplaner.model

data class TeamEvent(
    val type: TeamEventType,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val participants: List<TeamMember>
)

enum class TeamEventType(val label: String) {
    Training("Training"),
    Game("Spiel"),
    Other("Sonstiges")
}
