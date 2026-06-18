package de.teamplaner.data.auth

import de.teamplaner.data.ApiConfig
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.BufferedReader
import java.io.InputStreamReader
import java.net.HttpURLConnection
import java.net.URL

class AuthApiClient(
    private val baseUrl: String = ApiConfig.BASE_URL
) {
    suspend fun register(
        firstName: String,
        lastName: String,
        email: String,
        password: String
    ): Result<AuthSession> {
        return postAuth(
            path = "/auth/register",
            body = JSONObject()
                .put("firstName", firstName)
                .put("lastName", lastName)
                .put("email", email)
                .put("password", password)
        )
    }

    suspend fun login(
        email: String,
        password: String
    ): Result<AuthSession> {
        return postAuth(
            path = "/auth/login",
            body = JSONObject()
                .put("email", email)
                .put("password", password)
        )
    }

    suspend fun me(token: String): Result<AuthUser> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = openConnection("/auth/me")
                connection.requestMethod = "GET"
                connection.setRequestProperty("Authorization", "Bearer $token")

                val response = readResponse(connection)
                if (connection.responseCode !in 200..299) {
                    error(response.optString("error", "Anmeldung ist nicht mehr gültig"))
                }

                decodeUser(response.getJSONObject("user"))
            }
        }
    }

    private suspend fun postAuth(
        path: String,
        body: JSONObject
    ): Result<AuthSession> {
        return withContext(Dispatchers.IO) {
            runCatching {
                val connection = openConnection(path)
                connection.requestMethod = "POST"
                connection.doOutput = true
                connection.outputStream.use { stream ->
                    stream.write(body.toString().toByteArray())
                }

                val response = readResponse(connection)
                if (connection.responseCode !in 200..299) {
                    error(response.optString("error", "Anfrage fehlgeschlagen"))
                }

                AuthSession(
                    token = response.getString("token"),
                    user = decodeUser(response.getJSONObject("user"))
                )
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
        val stream = if (connection.responseCode in 200..299) {
            connection.inputStream
        } else {
            connection.errorStream
        }
        val text = BufferedReader(InputStreamReader(stream)).use { it.readText() }
        return if (text.isBlank()) JSONObject() else JSONObject(text)
    }

    private fun decodeUser(json: JSONObject): AuthUser {
        return AuthUser(
            id = json.getString("id"),
            name = json.getString("name"),
            email = json.getString("email")
        )
    }
}
