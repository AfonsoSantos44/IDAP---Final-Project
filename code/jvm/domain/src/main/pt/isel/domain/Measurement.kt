package pt.isel.domain

data class Measurement(
    val measurementId: Int,
    val analysisId: Int,
    val evidenceId: Int,
    val damageId: Int,
    val refObjLengthCm: Double,
    val refObjX1: Double,
    val refObjY1: Double,
    val refObjX2: Double,
    val refObjY2: Double,
    val damageAreaX1: Double,
    val damageAreaY1: Double,
    val damageAreaX2: Double,
    val damageAreaY2: Double
)