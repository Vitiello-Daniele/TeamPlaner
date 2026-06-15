package de.teamplaner.data

import android.content.Context
import de.teamplaner.model.Duty
import de.teamplaner.model.DutyAssignment
import de.teamplaner.model.DutyType
import de.teamplaner.model.Team
import de.teamplaner.model.TeamEvent
import de.teamplaner.model.TeamEventType
import de.teamplaner.model.TeamMember
import de.teamplaner.model.TeamRole
import de.teamplaner.model.Teilnahme
import de.teamplaner.model.TeilnahmeStatus
import org.json.JSONArray
import org.json.JSONObject

class LocalTeamPlanerRepository(
    context: Context,
    displayName: String,
    private val fallbackRepository: TeamPlanerRepository = FakeTeamPlanerRepository()
) : TeamPlanerRepository {
    private val preferences = context.getSharedPreferences("team_planer", Context.MODE_PRIVATE)
    private val dataKey = "data_shared"
    private val oldDataKey = "data_${displayName.lowercase().replace(Regex("[^a-z0-9]+"), "_")}"

    override fun loadData(displayName: String): TeamPlanerData {
        val sharedData = preferences.getString(dataKey, null)
        val oldData = preferences.getString(oldDataKey, null)
        val rawData = sharedData ?: oldData

        if (rawData.isNullOrBlank()) {
            return fallbackRepository.loadData(displayName)
        }

        return runCatching {
            decodeData(JSONObject(rawData))
                .also { data ->
                    if (sharedData == null && oldData != null) {
                        saveData(data)
                    }
                }
        }.getOrElse {
            fallbackRepository.loadData(displayName)
        }
    }

    override fun saveData(data: TeamPlanerData) {
        preferences.edit()
            .putString(dataKey, encodeData(data).toString())
            .apply()
    }

    private fun encodeData(data: TeamPlanerData): JSONObject {
        return JSONObject()
            .put("teams", JSONArray(data.teams.map(::encodeTeam)))
            .put("events", JSONArray(data.events.map(::encodeEvent)))
            .put("duties", JSONArray(data.duties.map(::encodeDuty)))
            .put("assignments", JSONArray(data.assignments.map(::encodeAssignment)))
    }

    private fun decodeData(json: JSONObject): TeamPlanerData {
        return TeamPlanerData(
            teams = json.optJSONArray("teams").toList(::decodeTeam),
            events = json.optJSONArray("events").toList(::decodeEvent),
            duties = json.optJSONArray("duties").toList(::decodeDuty),
            assignments = json.optJSONArray("assignments").toList(::decodeAssignment)
        )
    }

    private fun encodeTeam(team: Team): JSONObject {
        return JSONObject()
            .put("id", team.id)
            .put("name", team.name)
            .put("inviteCode", team.inviteCode)
            .put("inviteCodeActive", team.inviteCodeActive)
            .put("members", JSONArray(team.members.map(::encodeMember)))
    }

    private fun decodeTeam(json: JSONObject): Team {
        return Team(
            id = json.getString("id"),
            name = json.getString("name"),
            inviteCode = json.getString("inviteCode"),
            inviteCodeActive = json.getBoolean("inviteCodeActive"),
            members = json.optJSONArray("members").toList(::decodeMember)
        )
    }

    private fun encodeMember(member: TeamMember): JSONObject {
        return JSONObject()
            .put("id", member.id)
            .put("name", member.name)
            .put("role", member.role.name)
    }

    private fun decodeMember(json: JSONObject): TeamMember {
        return TeamMember(
            id = json.getString("id"),
            name = json.getString("name"),
            role = TeamRole.valueOf(json.getString("role"))
        )
    }

    private fun encodeEvent(event: TeamEvent): JSONObject {
        return JSONObject()
            .put("id", event.id)
            .put("teamId", event.teamId)
            .put("type", event.type.name)
            .put("title", event.title)
            .put("date", event.date)
            .put("time", event.time)
            .put("location", event.location)
            .put("dutyIds", JSONArray(event.dutyIds))
            .put("teilnahmen", JSONArray(event.teilnahmen.map(::encodeTeilnahme)))
    }

    private fun decodeEvent(json: JSONObject): TeamEvent {
        return TeamEvent(
            id = json.getString("id"),
            teamId = json.optString("teamId"),
            type = TeamEventType.valueOf(json.getString("type")),
            title = json.getString("title"),
            date = json.getString("date"),
            time = json.getString("time"),
            location = json.optString("location"),
            dutyIds = json.optJSONArray("dutyIds").toStringList(),
            teilnahmen = json.optJSONArray("teilnahmen").toList(::decodeTeilnahme)
        )
    }

    private fun encodeTeilnahme(teilnahme: Teilnahme): JSONObject {
        return JSONObject()
            .put("memberId", teilnahme.memberId)
            .put("status", teilnahme.status.name)
    }

    private fun decodeTeilnahme(json: JSONObject): Teilnahme {
        return Teilnahme(
            memberId = json.getString("memberId"),
            status = TeilnahmeStatus.valueOf(json.getString("status"))
        )
    }

    private fun encodeDuty(duty: Duty): JSONObject {
        return JSONObject()
            .put("id", duty.id)
            .put("teamId", duty.teamId)
            .put("type", duty.type.name)
            .put("title", duty.title)
            .put("description", duty.description)
    }

    private fun decodeDuty(json: JSONObject): Duty {
        return Duty(
            id = json.getString("id"),
            teamId = json.optString("teamId"),
            type = DutyType.valueOf(json.getString("type")),
            title = json.getString("title"),
            description = json.optString("description")
        )
    }

    private fun encodeAssignment(assignment: DutyAssignment): JSONObject {
        return JSONObject()
            .put("id", assignment.id)
            .put("eventId", assignment.eventId)
            .put("dutyId", assignment.dutyId)
            .put("memberId", assignment.memberId)
    }

    private fun decodeAssignment(json: JSONObject): DutyAssignment {
        return DutyAssignment(
            id = json.getString("id"),
            eventId = json.getString("eventId"),
            dutyId = json.getString("dutyId"),
            memberId = json.getString("memberId")
        )
    }

    private fun <T> JSONArray?.toList(mapper: (JSONObject) -> T): List<T> {
        if (this == null) {
            return emptyList()
        }

        return List(length()) { index ->
            mapper(getJSONObject(index))
        }
    }

    private fun JSONArray?.toStringList(): List<String> {
        if (this == null) {
            return emptyList()
        }

        return List(length()) { index ->
            getString(index)
        }
    }
}
