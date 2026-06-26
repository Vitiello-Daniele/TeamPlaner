package de.teamplaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.teamplaner.ui.TeamPlanerApp

// Login-Activity (Launcher): Start, Login, Registrierung.
// Nach erfolgreicher Anmeldung: AppActivity starten, diese Activity schließen.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            TeamPlanerApp(onAuthenticated = { token, name ->
                startActivity(AppActivity.intent(this@MainActivity, token, name))
                finish()
            })
        }
    }
}
