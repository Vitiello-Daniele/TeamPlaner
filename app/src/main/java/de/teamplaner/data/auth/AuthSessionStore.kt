package de.teamplaner.data.auth

import android.content.Context

class AuthSessionStore(context: Context) {
    private val preferences = context.getSharedPreferences("auth_session", Context.MODE_PRIVATE)

    fun load(): AuthSession? {
        val token = preferences.getString(KEY_TOKEN, null)
        val userId = preferences.getString(KEY_USER_ID, null)
        val name = preferences.getString(KEY_NAME, null)
        val email = preferences.getString(KEY_EMAIL, null)

        if (token.isNullOrBlank() || userId.isNullOrBlank() || name.isNullOrBlank() || email.isNullOrBlank()) {
            return null
        }

        return AuthSession(
            token = token,
            user = AuthUser(
                id = userId,
                name = name,
                email = email
            )
        )
    }

    fun save(session: AuthSession) {
        preferences.edit()
            .putString(KEY_TOKEN, session.token)
            .putString(KEY_USER_ID, session.user.id)
            .putString(KEY_NAME, session.user.name)
            .putString(KEY_EMAIL, session.user.email)
            .apply()
    }

    fun clear() {
        preferences.edit().clear().apply()
    }

    private companion object {
        const val KEY_TOKEN = "token"
        const val KEY_USER_ID = "user_id"
        const val KEY_NAME = "name"
        const val KEY_EMAIL = "email"
    }
}
