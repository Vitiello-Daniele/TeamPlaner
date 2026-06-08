package de.teamplaner.model

data class TeamEvent(
    val type: TeamEventType,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val teilnahmen: List<Teilnahme>
)

enum class TeamEventType(val label: String) {
    Training("Training"),
    Game("Spiel"),
    Other("Sonstiges")
}

data class Teilnahme(
    val member: TeamMember,
    val status: TeilnahmeStatus
)

enum class TeilnahmeStatus(val label: String) {
    Offen("Offen"),
    Zugesagt("Zugesagt"),
    Abgesagt("Abgesagt")
}
