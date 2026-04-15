package pt.isel.domain

data class AccidentScene(
    val sceneId: Int,
    val caseId: Int,
    val latitude: Double,
    val longitude: Double,
    val terrainInclination: Double,
    val roadGradient: Double,
    val roadType: String,
    val spatialDescription: String,
    val vehiclePositioningNotes: String
)