package pt.isel.http.dto

data class WeatherConditionsOutputDto(
    val weatherId: Int,
    val caseId: Int,
    val conditionType: String?,
    val temperature: Double?,
    val visibility: Double?,
    val precipitation: String?,
)

data class RefreshWeatherConditionsRequestDto(
    val latitude: Double? = null,
    val longitude: Double? = null,
)

data class AccidentSceneOutputDto(
    val sceneId: Int,
    val caseId: Int,
    val latitude: Double,
    val longitude: Double,
    val terrainInclination: Double,
    val roadGradient: Double,
    val roadType: String,
    val spatialDescription: String,
    val vehiclePositioningNotes: String,
)

data class RefreshAccidentSceneRequestDto(
    val latitude: Double,
    val longitude: Double,
)

data class VehicleOutputDto(
    val vehicleId: Int,
    val caseId: Int,
    val brand: String,
    val model: String,
    val yearOfFabrication: Int,
    val licensePlate: String,
    val role: String?,
)

data class CreateVehicleRequestDto(
    val brand: String,
    val model: String,
    val yearOfFabrication: Int,
    val licensePlate: String,
    val role: String? = null,
)

data class UpdateVehicleRequestDto(
    val brand: String? = null,
    val model: String? = null,
    val yearOfFabrication: Int? = null,
    val licensePlate: String? = null,
    val role: String? = null,
)

data class DamageOutputDto(
    val damageId: Int,
    val vehicleId: Int,
    val contactZone: String,
    val deformationType: String,
    val direction: String,
    val heightCm: Double?,
    val damageDescription: String,
)

data class CreateDamageRequestDto(
    val contactZone: String,
    val deformationType: String,
    val direction: String,
    val damageDescription: String,
)

data class UpdateDamageRequestDto(
    val contactZone: String? = null,
    val deformationType: String? = null,
    val direction: String? = null,
    val damageDescription: String? = null,
)

data class EvidenceOutputDto(
    val evidenceId: Int,
    val caseId: Int,
    val evidenceType: String,
    val evidenceDescription: String,
    val uploadedBy: Int,
    val uploadedAt: String,
)

data class CreateEvidenceRequestDto(
    val evidenceType: String,
    val evidenceDescription: String,
)

data class UpdateEvidenceRequestDto(
    val evidenceType: String? = null,
    val evidenceDescription: String? = null,
)

data class ImageEvidenceOutputDto(
    val imageEvidenceId: Int,
    val evidenceId: Int,
    val vehicleId: Int,
    val filePath: String,
    val width: Int,
    val height: Int,
    val metadata: String?,
)

data class AnalysisOutputDto(
    val analysisId: Int,
    val caseId: Int,
    val analystId: Int,
    val createdAt: String,
)

data class AnalysisImageOutputDto(
    val analysisId: Int,
    val evidenceId: Int,
    val purpose: String?,
)

data class UpsertAnalysisImageRequestDto(
    val evidenceId: Int,
    val purpose: String? = null,
)

data class MeasurementOutputDto(
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
    val damageAreaY2: Double,
    val calculatedHeightCm: Double,
    val damageMinHeightCm: Double,
    val damageMaxHeightCm: Double,
    val scaleCmPerPixel: Double,
    val confidence: Double,
    val calibrationMethod: String,
    val comparisonImagePath: String?,
    val processedAt: String,
)

data class CreateMeasurementRequestDto(
    val evidenceId: Int,
    val damageId: Int,
    val comparisonEvidenceId: Int? = null,
    val knownTickDistanceCm: Double? = null,
    val primarySelection: DamageSelectionRequestDto,
    val primaryCalibration: RulerCalibrationRequestDto? = null,
    val comparisonSelection: DamageSelectionRequestDto? = null,
    val comparisonCalibration: RulerCalibrationRequestDto? = null,
)

data class DamageSelectionRequestDto(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
)

data class RulerReferencePointRequestDto(
    val x: Double,
    val y: Double,
    val valueCm: Double,
)

data class RulerCalibrationRequestDto(
    val referencePoints: List<RulerReferencePointRequestDto> = emptyList(),
    val rulerRegion: DamageSelectionRequestDto? = null,
)

data class DamageComparisonOutputDto(
    val comparisonId: Int,
    val analysisId: Int,
    val damageSourceId: Int,
    val damageTargetId: Int,
    val compatibilityStatus: String,
    val notes: String?,
)

data class CreateDamageComparisonRequestDto(
    val damageSourceId: Int,
    val damageTargetId: Int,
    val compatibilityStatus: String,
    val notes: String? = null,
)

data class UpdateDamageComparisonRequestDto(
    val damageSourceId: Int? = null,
    val damageTargetId: Int? = null,
    val compatibilityStatus: String? = null,
    val notes: String? = null,
)

data class AnalysisConclusionOutputDto(
    val conclusionId: Int,
    val analysisId: Int,
    val compatibilityResult: String,
    val justification: String,
)

data class UpsertAnalysisConclusionRequestDto(
    val compatibilityResult: String,
    val justification: String,
)

data class ReportOutputDto(
    val reportId: Int,
    val analysisId: Int,
    val generatedAt: String,
    val filePath: String,
)

data class CreateReportRequestDto(
    val filePath: String? = null,
)

data class UpdateReportRequestDto(
    val filePath: String? = null,
)
