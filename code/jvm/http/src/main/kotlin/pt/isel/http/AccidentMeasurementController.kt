package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.MediaType
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.SecurityPrincipal
import pt.isel.http.dto.CreateMeasurementRequestDto
import pt.isel.services.AccidentMeasurementService
import pt.isel.services.DamageSelectionInput
import pt.isel.services.Failure
import pt.isel.services.RulerCalibrationInput
import pt.isel.services.RulerReferencePointInput
import pt.isel.services.Success

@RestController
class AccidentMeasurementController(
    private val measurementService: AccidentMeasurementService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Analyses.MEASUREMENTS)
    fun getMeasurements(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = measurementService.getMeasurementsByAnalysisId(analysisId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Analyses.MEASUREMENTS)
    fun createMeasurement(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
        @RequestBody request: CreateMeasurementRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                measurementService.createMeasurement(
                    analysisId = analysisId,
                    evidenceId = request.evidenceId,
                    damageId = request.damageId,
                    comparisonEvidenceId = request.comparisonEvidenceId,
                    knownTickDistanceCm = request.knownTickDistanceCm,
                    primarySelection = request.primarySelection.toServiceInput(),
                    primaryCalibration = request.primaryCalibration?.toServiceInput(),
                    comparisonSelection = request.comparisonSelection?.toServiceInput(),
                    comparisonCalibration = request.comparisonCalibration?.toServiceInput(),
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/measurements/${result.value.measurementId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    private fun pt.isel.http.dto.DamageSelectionRequestDto.toServiceInput() =
        DamageSelectionInput(
            x1 = x1,
            y1 = y1,
            x2 = x2,
            y2 = y2,
        )

    private fun pt.isel.http.dto.RulerCalibrationRequestDto.toServiceInput() =
        RulerCalibrationInput(
            referencePoints =
                referencePoints.map {
                    RulerReferencePointInput(
                        x = it.x,
                        y = it.y,
                        valueCm = it.valueCm,
                    )
                },
            rulerRegion = rulerRegion?.toServiceInput(),
        )

    @GetMapping(Uris.Measurements.GET_BY_ID)
    fun getMeasurement(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable measurementId: Int,
    ): ResponseEntity<*> =
        when (val access = accessControl.authorizeMeasurement(currentUser, measurementId)) {
            is MeasurementAccessResult.Authorized -> ResponseEntity.ok(access.measurement.toOutputDto())
            is MeasurementAccessResult.Rejected -> access.response
        }

    @GetMapping(Uris.Measurements.COMPARISON_IMAGE)
    fun getComparisonImage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable measurementId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeMeasurement(currentUser, measurementId)) {
            is MeasurementAccessResult.Authorized -> Unit
            is MeasurementAccessResult.Rejected -> return access.response
        }

        return when (val result = measurementService.getComparisonImageContent(measurementId)) {
            is Success ->
                ResponseEntity.ok()
                    .contentType(MediaType.parseMediaType(result.value.contentType))
                    .body(result.value.bytes)

            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Measurements.DELETE_BY_ID)
    fun deleteMeasurement(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable measurementId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeMeasurement(currentUser, measurementId)) {
            is MeasurementAccessResult.Authorized -> Unit
            is MeasurementAccessResult.Rejected -> return access.response
        }

        return when (val result = measurementService.deleteMeasurement(measurementId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
