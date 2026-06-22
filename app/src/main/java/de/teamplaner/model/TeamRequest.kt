package de.teamplaner.model

data class TeamRequest(
    val id: String = "",
    val teamId: String,
    val teamName: String = "",
    val userName: String,
    val type: TeamRequestType,
    val status: TeamRequestStatus
)

enum class TeamRequestType {
    JoinRequest,
    Invite
}

enum class TeamRequestStatus {
    Open,
    Accepted,
    Rejected
}
