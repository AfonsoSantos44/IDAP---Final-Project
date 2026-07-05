package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.stereotype.Component
import pt.isel.domain.AccidentCase
import pt.isel.domain.AccidentScene
import pt.isel.domain.Analysis
import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.AnalysisImage
import pt.isel.domain.Damage
import pt.isel.domain.DamageComparison
import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence
import pt.isel.domain.Measurement
import pt.isel.domain.Report
import pt.isel.domain.SecurityPrincipal
import pt.isel.domain.Vehicle
import pt.isel.domain.WeatherConditions
import pt.isel.http.dto.AccidentSceneOutputDto
import pt.isel.http.dto.AnalysisConclusionOutputDto
import pt.isel.http.dto.AnalysisImageOutputDto
import pt.isel.http.dto.AnalysisOutputDto
import pt.isel.http.dto.DamageComparisonOutputDto
import pt.isel.http.dto.DamageOutputDto
import pt.isel.http.dto.EvidenceOutputDto
import pt.isel.http.dto.ImageEvidenceOutputDto
import pt.isel.http.dto.MeasurementOutputDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.ReportOutputDto
import pt.isel.http.dto.VehicleOutputDto
import pt.isel.http.dto.WeatherConditionsOutputDto
import pt.isel.services.AccidentAnalysisService
import pt.isel.services.AccidentConclusionReportService
import pt.isel.services.AccidentDamageComparisonService
import pt.isel.services.AccidentDataError
import pt.isel.services.AccidentEvidenceService
import pt.isel.services.AccidentMeasurementService
import pt.isel.services.AccidentVehicleDamageService
import pt.isel.services.CaseError
import pt.isel.services.CaseService
import pt.isel.services.Failure
import pt.isel.services.Success
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@Component
class AccidentDataAccessControl(
    private val caseService: CaseService,
    private val vehicleDamageService: AccidentVehicleDamageService,
    private val evidenceService: AccidentEvidenceService,
    private val analysisService: AccidentAnalysisService,
    private val measurementService: AccidentMeasurementService,
    private val damageComparisonService: AccidentDamageComparisonService,
    private val conclusionReportService: AccidentConclusionReportService,
) {
    internal fun authorizeCase(
        currentUser: SecurityPrincipal?,
        caseId: Int,
    ): CaseAccessResult {
        val authenticatedUser =
            currentUser
                ?: return CaseAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))

        val accidentCase =
            when (val result = caseService.getCaseById(caseId)) {
                is Success -> result.value
                is Failure -> return CaseAccessResult.Rejected(result.value.toProblemResponse())
            }

        if (!authenticatedUser.canAccess(accidentCase)) {
            return CaseAccessResult.Rejected(Problem.CaseAccessDenied.response(HttpStatus.FORBIDDEN))
        }

        return CaseAccessResult.Authorized(authenticatedUser, accidentCase)
    }

    internal fun authorizeVehicle(
        currentUser: SecurityPrincipal?,
        vehicleId: Int,
    ): VehicleAccessResult {
        if (currentUser == null) {
            return VehicleAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val vehicle =
            when (val result = vehicleDamageService.getVehicleById(vehicleId)) {
                is Success -> result.value
                is Failure -> return VehicleAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeCase(currentUser, vehicle.caseId)) {
            is CaseAccessResult.Authorized -> VehicleAccessResult.Authorized(access.currentUser, vehicle)
            is CaseAccessResult.Rejected -> VehicleAccessResult.Rejected(access.response)
        }
    }

    internal fun authorizeDamage(
        currentUser: SecurityPrincipal?,
        damageId: Int,
    ): DamageAccessResult {
        if (currentUser == null) {
            return DamageAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val damage =
            when (val result = vehicleDamageService.getDamageById(damageId)) {
                is Success -> result.value
                is Failure -> return DamageAccessResult.Rejected(result.value.toProblemResponse())
            }
        val vehicle =
            when (val result = vehicleDamageService.getVehicleById(damage.vehicleId)) {
                is Success -> result.value
                is Failure -> return DamageAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeCase(currentUser, vehicle.caseId)) {
            is CaseAccessResult.Authorized -> DamageAccessResult.Authorized(access.currentUser, damage)
            is CaseAccessResult.Rejected -> DamageAccessResult.Rejected(access.response)
        }
    }

    internal fun authorizeEvidence(
        currentUser: SecurityPrincipal?,
        evidenceId: Int,
    ): EvidenceAccessResult {
        if (currentUser == null) {
            return EvidenceAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val evidence =
            when (val result = evidenceService.getEvidenceById(evidenceId)) {
                is Success -> result.value
                is Failure -> return EvidenceAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeCase(currentUser, evidence.caseId)) {
            is CaseAccessResult.Authorized -> EvidenceAccessResult.Authorized(access.currentUser, evidence)
            is CaseAccessResult.Rejected -> EvidenceAccessResult.Rejected(access.response)
        }
    }

    internal fun authorizeAnalysis(
        currentUser: SecurityPrincipal?,
        analysisId: Int,
    ): AnalysisAccessResult {
        if (currentUser == null) {
            return AnalysisAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val analysis =
            when (val result = analysisService.getAnalysisById(analysisId)) {
                is Success -> result.value
                is Failure -> return AnalysisAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeCase(currentUser, analysis.caseId)) {
            is CaseAccessResult.Authorized -> AnalysisAccessResult.Authorized(access.currentUser, analysis)
            is CaseAccessResult.Rejected -> AnalysisAccessResult.Rejected(access.response)
        }
    }

    internal fun authorizeMeasurement(
        currentUser: SecurityPrincipal?,
        measurementId: Int,
    ): MeasurementAccessResult {
        if (currentUser == null) {
            return MeasurementAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val measurement =
            when (val result = measurementService.getMeasurementById(measurementId)) {
                is Success -> result.value
                is Failure -> return MeasurementAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeAnalysis(currentUser, measurement.analysisId)) {
            is AnalysisAccessResult.Authorized -> MeasurementAccessResult.Authorized(access.currentUser, measurement)
            is AnalysisAccessResult.Rejected -> MeasurementAccessResult.Rejected(access.response)
        }
    }

    internal fun authorizeDamageComparison(
        currentUser: SecurityPrincipal?,
        comparisonId: Int,
    ): DamageComparisonAccessResult {
        if (currentUser == null) {
            return DamageComparisonAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val damageComparison =
            when (val result = damageComparisonService.getDamageComparisonById(comparisonId)) {
                is Success -> result.value
                is Failure -> return DamageComparisonAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeAnalysis(currentUser, damageComparison.analysisId)) {
            is AnalysisAccessResult.Authorized ->
                DamageComparisonAccessResult.Authorized(access.currentUser, damageComparison)

            is AnalysisAccessResult.Rejected -> DamageComparisonAccessResult.Rejected(access.response)
        }
    }

    internal fun authorizeReport(
        currentUser: SecurityPrincipal?,
        reportId: Int,
    ): ReportAccessResult {
        if (currentUser == null) {
            return ReportAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))
        }

        val report =
            when (val result = conclusionReportService.getReportById(reportId)) {
                is Success -> result.value
                is Failure -> return ReportAccessResult.Rejected(result.value.toProblemResponse())
            }

        return when (val access = authorizeAnalysis(currentUser, report.analysisId)) {
            is AnalysisAccessResult.Authorized -> ReportAccessResult.Authorized(access.currentUser, report)
            is AnalysisAccessResult.Rejected -> ReportAccessResult.Rejected(access.response)
        }
    }
}

internal fun CaseError.toProblemResponse(): ResponseEntity<Any> =
    when (this) {
        is CaseError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
        is CaseError.CaseNotFound -> Problem.CaseNotFound.response(HttpStatus.NOT_FOUND)
        is CaseError.InvalidCaseStatus -> Problem.InvalidCaseStatus.response(HttpStatus.BAD_REQUEST)
        is CaseError.InvalidCaseDescription -> Problem.InvalidCaseDescription.response(HttpStatus.BAD_REQUEST)
    }

internal fun AccidentDataError.toProblemResponse(): ResponseEntity<Any> =
    when (this) {
        is AccidentDataError.CaseNotFound -> Problem.CaseNotFound.response(HttpStatus.NOT_FOUND)
        is AccidentDataError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
        is AccidentDataError.DuplicateVehicle -> Problem.DuplicateVehicle.response(HttpStatus.CONFLICT)
        is AccidentDataError.InvalidAccidentData -> Problem.InvalidAccidentData.response(HttpStatus.BAD_REQUEST)
        is AccidentDataError.MeasurementProcessingFailed ->
            Problem.MeasurementProcessingFailed(this.detail).response(HttpStatus.BAD_REQUEST)

        is AccidentDataError.ReportGenerationFailed ->
            Problem.ReportGenerationFailed.response(HttpStatus.INTERNAL_SERVER_ERROR)

        is AccidentDataError.ExternalDataUnavailable ->
            Problem.ExternalDataUnavailable.response(HttpStatus.BAD_GATEWAY)

        is AccidentDataError.RelatedResourceMismatch ->
            Problem.RelatedResourceMismatch.response(HttpStatus.BAD_REQUEST)

        is AccidentDataError.WeatherNotFound,
        is AccidentDataError.SceneNotFound,
        is AccidentDataError.VehicleNotFound,
        is AccidentDataError.DamageNotFound,
        is AccidentDataError.EvidenceNotFound,
        is AccidentDataError.ImageEvidenceNotFound,
        is AccidentDataError.AnalysisNotFound,
        is AccidentDataError.AnalysisImageNotFound,
        is AccidentDataError.MeasurementNotFound,
        is AccidentDataError.DamageComparisonNotFound,
        is AccidentDataError.AnalysisConclusionNotFound,
        is AccidentDataError.ReportNotFound,
        -> Problem.AccidentDataNotFound.response(HttpStatus.NOT_FOUND)
    }

internal fun WeatherConditions.toOutputDto() =
    WeatherConditionsOutputDto(
        weatherId = weatherId,
        caseId = caseId,
        conditionType = conditionType,
        temperature = temperature,
        visibility = visibility,
        precipitation = precipitation,
    )

internal fun AccidentScene.toOutputDto() =
    AccidentSceneOutputDto(
        sceneId = sceneId,
        caseId = caseId,
        latitude = latitude,
        longitude = longitude,
        terrainInclination = terrainInclination,
        roadGradient = roadGradient,
        roadType = roadType,
        spatialDescription = spatialDescription,
        vehiclePositioningNotes = vehiclePositioningNotes,
    )

internal fun Vehicle.toOutputDto() =
    VehicleOutputDto(
        vehicleId = vehicleId,
        caseId = caseId,
        brand = brand,
        model = model,
        yearOfFabrication = yearOfFabrication,
        licensePlate = licensePlate,
        role = role,
    )

internal fun Damage.toOutputDto() =
    DamageOutputDto(
        damageId = damageId,
        vehicleId = vehicleId,
        contactZone = contactZone,
        deformationType = deformationType,
        direction = direction,
        heightCm = heightCm,
        damageDescription = damageDescription,
    )

internal fun Evidence.toOutputDto() =
    EvidenceOutputDto(
        evidenceId = evidenceId,
        caseId = caseId,
        evidenceType = evidenceType,
        evidenceDescription = evidenceDescription,
        uploadedBy = uploadedBy,
        uploadedAt = formatTimestamp(uploadedAt),
    )

internal fun ImageEvidence.toOutputDto() =
    ImageEvidenceOutputDto(
        imageEvidenceId = imageEvidenceId,
        evidenceId = evidenceId,
        vehicleId = vehicleId,
        filePath = filePath,
        width = width,
        height = height,
        metadata = metadata,
    )

internal fun Analysis.toOutputDto() =
    AnalysisOutputDto(
        analysisId = analysisId,
        caseId = caseId,
        analystId = analystId,
        createdAt = formatTimestamp(createdAt),
    )

internal fun AnalysisImage.toOutputDto() =
    AnalysisImageOutputDto(
        analysisId = analysisId,
        evidenceId = evidenceId,
        purpose = purpose,
    )

internal fun Measurement.toOutputDto() =
    MeasurementOutputDto(
        measurementId = measurementId,
        analysisId = analysisId,
        evidenceId = evidenceId,
        damageId = damageId,
        refObjLengthCm = refObjLengthCm,
        refObjX1 = refObjX1,
        refObjY1 = refObjY1,
        refObjX2 = refObjX2,
        refObjY2 = refObjY2,
        damageAreaX1 = damageAreaX1,
        damageAreaY1 = damageAreaY1,
        damageAreaX2 = damageAreaX2,
        damageAreaY2 = damageAreaY2,
        calculatedHeightCm = calculatedHeightCm,
        damageMinHeightCm = damageMinHeightCm,
        damageMaxHeightCm = damageMaxHeightCm,
        scaleCmPerPixel = scaleCmPerPixel,
        confidence = confidence,
        calibrationMethod = calibrationMethod,
        comparisonImagePath = comparisonImagePath,
        processedAt = formatTimestamp(processedAt),
    )

internal fun DamageComparison.toOutputDto() =
    DamageComparisonOutputDto(
        comparisonId = comparisonId,
        analysisId = analysisId,
        damageSourceId = damageSourceId,
        damageTargetId = damageTargetId,
        compatibilityStatus = compatibilityStatus,
        notes = notes,
    )

internal fun AnalysisConclusion.toOutputDto() =
    AnalysisConclusionOutputDto(
        conclusionId = conclusionId,
        analysisId = analysisId,
        compatibilityResult = compatibilityResult,
        justification = justification,
    )

internal fun Report.toOutputDto() =
    ReportOutputDto(
        reportId = reportId,
        analysisId = analysisId,
        generatedAt = formatTimestamp(generatedAt),
        filePath = filePath,
    )

private fun SecurityPrincipal.canAccess(accidentCase: AccidentCase): Boolean = isAdmin() || userId == accidentCase.userId

private fun formatTimestamp(timestamp: Instant): String = timestampFormatter.format(timestamp)

private val timestampFormatter: DateTimeFormatter =
    DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
        .withZone(ZoneId.systemDefault())

internal sealed class CaseAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val accidentCase: AccidentCase,
    ) : CaseAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : CaseAccessResult()
}

internal sealed class VehicleAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val vehicle: Vehicle,
    ) : VehicleAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : VehicleAccessResult()
}

internal sealed class DamageAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val damage: Damage,
    ) : DamageAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : DamageAccessResult()
}

internal sealed class EvidenceAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val evidence: Evidence,
    ) : EvidenceAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : EvidenceAccessResult()
}

internal sealed class AnalysisAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val analysis: Analysis,
    ) : AnalysisAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : AnalysisAccessResult()
}

internal sealed class MeasurementAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val measurement: Measurement,
    ) : MeasurementAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : MeasurementAccessResult()
}

internal sealed class DamageComparisonAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val damageComparison: DamageComparison,
    ) : DamageComparisonAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : DamageComparisonAccessResult()
}

internal sealed class ReportAccessResult {
    data class Authorized(
        val currentUser: SecurityPrincipal,
        val report: Report,
    ) : ReportAccessResult()

    data class Rejected(
        val response: ResponseEntity<Any>,
    ) : ReportAccessResult()
}
