package pt.isel.domain

data class AccidentSceneSnapshot(
    val latitude: Double,
    val longitude: Double,
    val terrainInclination: Double,
    val roadGradient: Double,
    val roadType: String,
    val spatialDescription: String,
    val vehiclePositioningNotes: String,
)
