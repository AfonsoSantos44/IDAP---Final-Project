package pt.isel.repository

import pt.isel.domain.Measurement

interface RepositoryAccidentMeasurement {
    fun createMeasurement(
        analysisId: Int,
        evidenceId: Int,
        damageId: Int,
        refObjLengthCm: Double,
        refObjX1: Double,
        refObjY1: Double,
        refObjX2: Double,
        refObjY2: Double,
        damageAreaX1: Double,
        damageAreaY1: Double,
        damageAreaX2: Double,
        damageAreaY2: Double,
        calculatedHeightCm: Double,
        damageMinHeightCm: Double,
        damageMaxHeightCm: Double,
        scaleCmPerPixel: Double,
        confidence: Double,
        calibrationMethod: String,
        comparisonImagePath: String?,
    ): Measurement

    fun findMeasurementById(measurementId: Int): Measurement?

    fun findMeasurementsByAnalysisId(analysisId: Int): List<Measurement>

    fun deleteMeasurementById(measurementId: Int): Int
}
