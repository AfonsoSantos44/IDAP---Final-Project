package pt.isel.domain

data class ComputedMeasurement(
    val refObjLengthCm: Double,
    val refObjX1: Double,
    val refObjY1: Double,
    val refObjX2: Double,
    val refObjY2: Double,
    val damageAreaX1: Double,
    val damageAreaY1: Double,
    val damageAreaX2: Double,
    val damageAreaY2: Double,
    val calculatedHeightCm: Double,
    val damageMinHeightCm: Double,
    val damageMaxHeightCm: Double,
    val scaleCmPerPixel: Double,
    val confidence: Double,
    val calibrationMethod: String,
    val comparisonImagePath: String?,
)
