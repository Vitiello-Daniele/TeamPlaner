package de.teamplaner.data

interface TeamPlanerRepository {
    fun loadData(displayName: String): TeamPlanerData

    fun saveData(data: TeamPlanerData)
}
