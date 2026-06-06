package pt.isel.services

import pt.isel.repository.Transaction

internal const val MAX_SHORT_TEXT = 255
internal const val MAX_STATUS_TEXT = 50
internal const val MAX_EVIDENCE_TYPE = 50
internal const val MAX_LICENSE_PLATE = 50
internal const val MAX_FILE_PATH = 255
internal const val MAX_ANALYSIS_IMAGE_PURPOSE = 50
internal const val MAX_LONG_TEXT = 5000
internal const val MIN_VEHICLE_YEAR = 1867
internal const val MAX_VEHICLE_YEAR = 2026

internal fun normalizeRequiredText(
    value: String?,
    maxLength: Int,
): String? {
    val normalized = value?.trim() ?: return null
    return normalized.takeIf { it.isNotEmpty() && it.length <= maxLength }
}

internal fun normalizeOptionalText(value: String?): String? = value?.trim()?.ifEmpty { null }

internal fun isValidOptionalText(
    value: String?,
    maxLength: Int,
): Boolean = value == null || value.trim().length <= maxLength

internal fun isValidLatitude(value: Double): Boolean = isValidNumber(value) && value >= -90.0 && value <= 90.0

internal fun isValidLongitude(value: Double): Boolean = isValidNumber(value) && value >= -180.0 && value <= 180.0

internal fun isValidPositiveNumber(value: Double): Boolean = isValidNumber(value) && value > 0.0

internal fun isValidVehicleYear(value: Int): Boolean = value in MIN_VEHICLE_YEAR..MAX_VEHICLE_YEAR

internal fun isValidNumber(value: Double): Boolean = !value.isNaN() && !value.isInfinite()

internal fun Transaction.ensureEvidenceAndDamageBelongToCase(
    caseId: Int,
    evidenceId: Int,
    damageId: Int,
): AccidentDataError? {
    val evidence = repoAccidentEvidence.findEvidenceById(evidenceId) ?: return AccidentDataError.EvidenceNotFound
    if (evidence.caseId != caseId) return AccidentDataError.RelatedResourceMismatch

    return ensureDamageBelongsToCase(caseId, damageId)
}

internal fun Transaction.ensureDamageBelongsToCase(
    caseId: Int,
    damageId: Int,
): AccidentDataError? {
    val damage = repoAccidentVehicleDamage.findDamageById(damageId) ?: return AccidentDataError.DamageNotFound
    val vehicle = repoAccidentVehicleDamage.findVehicleById(damage.vehicleId) ?: return AccidentDataError.VehicleNotFound

    return if (vehicle.caseId == caseId) null else AccidentDataError.RelatedResourceMismatch
}

internal data class MeasurementProcessingInput(
    val primaryImagePath: String,
    val comparisonImagePath: String?,
)

internal data class GeoCoordinates(
    val latitude: Double,
    val longitude: Double,
)
