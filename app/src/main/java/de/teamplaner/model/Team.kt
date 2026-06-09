package de.teamplaner.model

data class Team(
    val name: String,
    val inviteCode: String,
    val inviteCodeActive: Boolean,
    val members: List<TeamMember>
)

data class TeamMember(
    val name: String,
    val role: TeamRole
)

enum class TeamRole(val label: String) {
    Trainer("Trainer"),
    Member("Mitglied")
}
