package de.teamplaner.model

data class TeamEvent(
    val id: String = "",
    val teamId: String = "",
    val type: TeamEventType,
    val title: String,
    val date: String,
    val time: String,
    val location: String,
    val dutyIds: List<String> = emptyList(),
    val teilnahmen: List<Teilnahme>
)

enum class TeamEventType(val label: String) {
    Training("Training"),
    Game("Spiel"),
    Other("Sonstiges")
}

data class Teilnahme(
    val memberId: String,
    val status: TeilnahmeStatus
)

enum class TeilnahmeStatus(val label: String) {
    Offen("Offen"),
    Zugesagt("Zugesagt"),
    Abgesagt("Abgesagt")
}
