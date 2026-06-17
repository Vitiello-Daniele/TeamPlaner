package de.teamplaner.data.team

import de.teamplaner.data.TeamPlanerData
import de.teamplaner.model.Team
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRequest
import de.teamplaner.model.TeamRequestStatus
import de.teamplaner.model.TeamRequestType
import de.teamplaner.model.TeamRole
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
                events = emptyList(),
                duties = emptyList(),
                assignments = emptyList(),
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

    private fun <T> JSONArray.mapJsonObjects(transform: (JSONObject) -> T): List<T> {
        return List(length()) { index ->
            transform(getJSONObject(index))
        }
    }
}
