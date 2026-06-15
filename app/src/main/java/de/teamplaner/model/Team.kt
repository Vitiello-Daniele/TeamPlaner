package de.teamplaner.model

data class Team(
    val id: String = "",
    val name: String,
    val inviteCode: String,
    val inviteCodeActive: Boolean,
    val members: List<TeamMember>
)

data class TeamMember(
    val id: String = "",
    val name: String,
    val role: TeamRole
)

enum class TeamRole(val label: String) {
    Trainer("Trainer"),
    Member("Mitglied")
}
