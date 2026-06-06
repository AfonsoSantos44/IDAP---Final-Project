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
import pt.isel.http.dto.UpsertAnalysisImageRequestDto
import pt.isel.services.AccidentAnalysisService
import pt.isel.services.Failure
import pt.isel.services.Success

@RestController
class AccidentAnalysisController(
    private val analysisService: AccidentAnalysisService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Cases.ANALYSES)
    fun getAnalysesForCase(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = analysisService.getAnalysesByCaseId(caseId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Cases.ANALYSES)
    fun createAnalysis(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        val access =
            when (val result = accessControl.authorizeCase(currentUser, caseId)) {
                is CaseAccessResult.Authorized -> result
                is CaseAccessResult.Rejected -> return result.response
            }

        return when (val result = analysisService.createAnalysis(caseId, access.currentUser.userId)) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/analyses/${result.value.analysisId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Analyses.GET_BY_ID)
    fun getAnalysis(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> =
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> ResponseEntity.ok(access.analysis.toOutputDto())
            is AnalysisAccessResult.Rejected -> access.response
        }

    @DeleteMapping(Uris.Analyses.DELETE_BY_ID)
    fun deleteAnalysis(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = analysisService.deleteAnalysis(analysisId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Analyses.IMAGES)
    fun getAnalysisImages(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = analysisService.getAnalysisImagesByAnalysisId(analysisId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Analyses.IMAGES)
    fun upsertAnalysisImage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
        @RequestBody request: UpsertAnalysisImageRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                analysisService.upsertAnalysisImage(
                    analysisId = analysisId,
                    evidenceId = request.evidenceId,
                    purpose = request.purpose,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Analyses.IMAGE_BY_EVIDENCE_ID)
    fun deleteAnalysisImage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
        @PathVariable evidenceId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = analysisService.deleteAnalysisImage(analysisId, evidenceId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
