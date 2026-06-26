package de.teamplaner

import android.content.Context
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.teamplaner.data.auth.AuthSessionStore
import de.teamplaner.ui.screens.MainAppScreen
import de.teamplaner.ui.theme.TeamPlanerTheme

// Haupt-Activity (eingeloggt): bekommt Token und Name von der MainActivity.
// Die Ansichten darin sind Compose-Screens.
class AppActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val token = intent.getStringExtra(EXTRA_TOKEN).orEmpty()
        val name = intent.getStringExtra(EXTRA_NAME).orEmpty()
        setContent {
            TeamPlanerTheme {
                MainAppScreen(
                    name = name,
                    token = token,
                    onLogoutClick = {
                        // Logout: Sitzung löschen, zurück zur Login-Activity
                        AuthSessionStore(applicationContext).clear()
                        startActivity(Intent(this@AppActivity, MainActivity::class.java))
                        finish()
                    }
                )
            }
        }
    }

    companion object {
        private const val EXTRA_TOKEN = "extra_token"
        private const val EXTRA_NAME = "extra_name"

        // Intent mit Token und Name für die AppActivity bauen
        fun intent(context: Context, token: String, name: String): Intent =
            Intent(context, AppActivity::class.java).apply {
                putExtra(EXTRA_TOKEN, token)
                putExtra(EXTRA_NAME, name)
            }
    }
}
