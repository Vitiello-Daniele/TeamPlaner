package de.teamplaner

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import de.teamplaner.ui.TeamPlanerApp

// Einstiegspunkt der App.
// Bewusst nur eine Activity: die einzelnen Ansichten sind Compose-Screens
// (Navigation passiert in TeamPlanerApp), das entspricht dem aktuellen Android-Aufbau.
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // setContent verbindet die Activity mit der Compose-Oberfläche
        setContent {
            TeamPlanerApp()
        }
    }
}
