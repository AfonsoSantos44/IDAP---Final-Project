package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.SecurityPrincipal
import pt.isel.http.dto.CreateDamageComparisonRequestDto
import pt.isel.http.dto.UpdateDamageComparisonRequestDto
import pt.isel.services.AccidentDamageComparisonService
import pt.isel.services.Failure
import pt.isel.services.Success

@RestController
class AccidentDamageComparisonController(
    private val damageComparisonService: AccidentDamageComparisonService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Analyses.DAMAGE_COMPARISONS)
    fun getDamageComparisons(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = damageComparisonService.getDamageComparisonsByAnalysisId(analysisId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Analyses.DAMAGE_COMPARISONS)
    fun createDamageComparison(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
        @RequestBody request: CreateDamageComparisonRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                damageComparisonService.createDamageComparison(
                    analysisId = analysisId,
                    damageSourceId = request.damageSourceId,
                    damageTargetId = request.damageTargetId,
                    compatibilityStatus = request.compatibilityStatus,
                    notes = request.notes,
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/damage-comparisons/${result.value.comparisonId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.DamageComparisons.GET_BY_ID)
    fun getDamageComparison(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable comparisonId: Int,
    ): ResponseEntity<*> =
        when (val access = accessControl.authorizeDamageComparison(currentUser, comparisonId)) {
            is DamageComparisonAccessResult.Authorized -> ResponseEntity.ok(access.damageComparison.toOutputDto())
            is DamageComparisonAccessResult.Rejected -> access.response
        }

    @PutMapping(Uris.DamageComparisons.UPDATE_BY_ID)
    fun updateDamageComparison(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable comparisonId: Int,
        @RequestBody request: UpdateDamageComparisonRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeDamageComparison(currentUser, comparisonId)) {
            is DamageComparisonAccessResult.Authorized -> Unit
            is DamageComparisonAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                damageComparisonService.updateDamageComparison(
                    comparisonId = comparisonId,
                    damageSourceId = request.damageSourceId,
                    damageTargetId = request.damageTargetId,
                    compatibilityStatus = request.compatibilityStatus,
                    notes = request.notes,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.DamageComparisons.DELETE_BY_ID)
    fun deleteDamageComparison(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable comparisonId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeDamageComparison(currentUser, comparisonId)) {
            is DamageComparisonAccessResult.Authorized -> Unit
            is DamageComparisonAccessResult.Rejected -> return access.response
        }

        return when (val result = damageComparisonService.deleteDamageComparison(comparisonId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
