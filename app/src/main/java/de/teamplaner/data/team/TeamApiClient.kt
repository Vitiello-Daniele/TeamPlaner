package de.teamplaner.data.team

import de.teamplaner.data.TeamPlanerData
import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.DutyType
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamEventType
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRequest
import de.teamplaner.model.TeamRequestStatus
import de.teamplaner.model.TeamRequestType
import de.teamplaner.model.TeamRole
import de.teamplaner.model.Teilnahme
import de.teamplaner.model.TeilnahmeStatus
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class TeamApiClient(
    private val baseUrl: String = "http://10.0.2.2:3000"
) {
    suspend fun loadData(token: String): Result<TeamPlanerData> {
        return request(token = token, path = "/teams", method = "GET") { json ->
            TeamPlanerData(
                teams = json.getJSONArray("teams").mapJsonObjects(::decodeTeam),
                events = json.getJSONArray("events").mapJsonObjects(::decodeEvent),
                duties = json.getJSONArray("duties").mapJsonObjects(::decodeDuty),
                assignments = json.getJSONArray("assignments").mapJsonObjects(::decodeAssignment),
                requests = json.getJSONArray("requests").mapJsonObjects(::decodeRequest)
            )
        }
    }

    suspend fun createTeam(token: String, name: String): Result<Unit> {
        return request(
            token = token,
            path = "/teams",
            method = "POST",
            body = JSONObject().put("name", name)
        ) {}
    }

    suspend fun joinTeam(token: String, inviteCode: String): Result<Unit> {
        return request(
            token = token,
            path = "/teams/join",
            method = "POST",
            body = JSONObject().put("inviteCode", inviteCode)
        ) {}
    }

    suspend fun inviteMember(token: String, teamId: String, user: String): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/invites",
            method = "POST",
            body = JSONObject().put("user", user)
        ) {}
    }

    suspend fun updateRequest(token: String, requestId: String, status: TeamRequestStatus): Result<Unit> {
        return request(
            token = token,
            path = "/team-requests/$requestId",
            method = "PATCH",
            body = JSONObject().put("status", status.name)
        ) {}
    }

    suspend fun removeMember(token: String, teamId: String, memberId: String): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/members/$memberId",
            method = "DELETE"
        ) {}
    }

    suspend fun refreshInviteCode(token: String, teamId: String): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/invite-code",
            method = "PATCH",
            body = JSONObject().put("action", "refresh")
        ) {}
    }

    suspend fun deactivateInviteCode(token: String, teamId: String): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/invite-code",
            method = "PATCH",
            body = JSONObject().put("action", "deactivate")
        ) {}
    }

    suspend fun createDuty(token: String, teamId: String, duty: Duty): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/duties",
            method = "POST",
            body = encodeDuty(duty)
        ) {}
    }

    suspend fun removeDuty(token: String, dutyId: String): Result<Unit> {
        return request(
            token = token,
            path = "/duties/$dutyId",
            method = "DELETE"
        ) {}
    }

    suspend fun createEvent(token: String, teamId: String, event: TeamEvent): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/events",
            method = "POST",
            body = encodeEvent(event)
                .put("participantIds", JSONArray(event.teilnahmen.map { it.memberId }))
        ) {}
    }

    suspend fun updateEvent(token: String, event: TeamEvent): Result<Unit> {
        return request(
            token = token,
            path = "/events/${event.id}",
            method = "PATCH",
            body = encodeEvent(event)
        ) {}
    }

    suspend fun removeEvent(token: String, eventId: String): Result<Unit> {
        return request(
            token = token,
            path = "/events/$eventId",
            method = "DELETE"
        ) {}
    }

    suspend fun assignDuty(
        token: String,
        event: TeamEvent,
        duty: Duty,
        member: TeamMember
    ): Result<Unit> {
        return request(
            token = token,
            path = "/assignments",
            method = "POST",
            body = JSONObject()
                .put("eventId", event.id)
                .put("dutyId", duty.id)
                .put("memberId", member.id)
        ) {}
    }

    suspend fun removeAssignment(token: String, assignmentId: String): Result<Unit> {
        return request(
            token = token,
            path = "/assignments/$assignmentId",
            method = "DELETE"
        ) {}
    }

    suspend fun createFairPlan(
        token: String,
        teamId: String,
        replaceExisting: Boolean
    ): Result<Unit> {
        return request(
            token = token,
            path = "/teams/$teamId/assignments/fair",
            method = "POST",
            body = JSONObject().put("replaceExisting", replaceExisting)
        ) {}
    }

    private suspend fun <T> request(
        token: String,
        path: String,
        method: String,
        body: JSONObject? = null,
        decode: (JSONObject) -> T
    ): Result<T> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = openConnection(path)
                connection.requestMethod = method
                connection.setRequestProperty("Authorization", "Bearer $token")

                if (body != null) {
                    connection.doOutput = true
                    connection.outputStream.use { stream ->
                        stream.write(body.toString().toByteArray())
                    }
                }

                val response = readResponse(connection)
                if (connection.responseCode !in 200..299) {
                    error(response.optString("error", "Anfrage fehlgeschlagen"))
                }

                decode(response)
            }
        }
    }

    private fun openConnection(path: String): HttpURLConnection {
        return (URL("$baseUrl$path").openConnection() as HttpURLConnection).apply {
            connectTimeout = 5000
            readTimeout = 5000
            setRequestProperty("Content-Type", "application/json")
            setRequestProperty("Accept", "application/json")
        }
    }

    private fun readResponse(connection: HttpURLConnection): JSONObject {
        if (connection.responseCode == 204) {
            return JSONObject()
        }

        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun decodeTeam(json: JSONObject): Team {
        return Team(
            id = json.getString("id"),
            name = json.getString("name"),
            inviteCode = json.getString("inviteCode"),
            inviteCodeActive = json.getBoolean("inviteCodeActive"),
            members = json.getJSONArray("members").mapJsonObjects { member ->
                TeamMember(
                    id = member.getString("id"),
                    name = member.getString("name"),
                    role = TeamRole.valueOf(member.getString("role"))
                )
            }
        )
    }

    private fun decodeRequest(json: JSONObject): TeamRequest {
        return TeamRequest(
            id = json.getString("id"),
            teamId = json.getString("teamId"),
            userName = json.getString("userName"),
            type = TeamRequestType.valueOf(json.getString("type")),
            status = TeamRequestStatus.valueOf(json.getString("status"))
        )
    }

    private fun decodeDuty(json: JSONObject): Duty {
        return Duty(
            id = json.getString("id"),
            teamId = json.getString("teamId"),
            type = DutyType.valueOf(json.getString("type")),
            title = json.getString("title"),
            description = json.getString("description")
        )
    }

    private fun decodeEvent(json: JSONObject): TeamEvent {
        return TeamEvent(
            id = json.getString("id"),
            teamId = json.getString("teamId"),
            type = TeamEventType.valueOf(json.getString("type")),
            title = json.getString("title"),
            date = json.getString("date"),
            time = json.getString("time"),
            location = json.getString("location"),
            dutyIds = json.getJSONArray("dutyIds").mapStrings(),
            teilnahmen = json.getJSONArray("teilnahmen").mapJsonObjects { teilnahme ->
                Teilnahme(
                    memberId = teilnahme.getString("memberId"),
                    status = TeilnahmeStatus.valueOf(teilnahme.getString("status"))
                )
            }
        )
    }

    private fun decodeAssignment(json: JSONObject): DutyAssignment {
        return DutyAssignment(
            id = json.getString("id"),
            eventId = json.getString("eventId"),
            dutyId = json.getString("dutyId"),
            memberId = json.getString("memberId")
        )
    }

    private fun encodeDuty(duty: Duty): JSONObject {
        return JSONObject()
            .put("type", duty.type.name)
            .put("title", duty.title)
            .put("description", duty.description)
    }

    private fun encodeEvent(event: TeamEvent): JSONObject {
        return JSONObject()
            .put("type", event.type.name)
            .put("title", event.title)
            .put("date", event.date)
            .put("time", event.time)
            .put("location", event.location)
            .put("dutyIds", JSONArray(event.dutyIds))
            .put(
                "teilnahmen",
                JSONArray(
                    event.teilnahmen.map { teilnahme ->
                        JSONObject()
                            .put("memberId", teilnahme.memberId)
                            .put("status", teilnahme.status.name)
                    }
                )
            )
    }

    private fun <T> JSONArray.mapJsonObjects(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index ->
            transform(getJSONObject(index))
        }
    }

    private fun JSONArray.mapStrings(): List<String> {
        return List(length()) { index ->
            getString(index)
        }
    }
}
