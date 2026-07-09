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
import pt.isel.http.dto.CreateReportRequestDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.UpdateReportRequestDto
import pt.isel.http.dto.UpsertAnalysisConclusionRequestDto
import pt.isel.services.AccidentConclusionReportService
import pt.isel.services.Failure
import pt.isel.services.Success

@RestController
class AccidentConclusionReportController(
    private val conclusionReportService: AccidentConclusionReportService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Analyses.CONCLUSION)
    fun getAnalysisConclusion(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = conclusionReportService.getAnalysisConclusionByAnalysisId(analysisId)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Analyses.CONCLUSION)
    fun upsertAnalysisConclusion(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
        @RequestBody request: UpsertAnalysisConclusionRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                conclusionReportService.upsertAnalysisConclusion(
                    analysisId = analysisId,
                    compatibilityResult = request.compatibilityResult,
                    justification = request.justification,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Analyses.CONCLUSION)
    fun deleteAnalysisConclusion(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = conclusionReportService.deleteAnalysisConclusion(analysisId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Reports.LIST)
    fun listReports(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
    ): ResponseEntity<*> {
        val authenticatedUser = currentUser ?: return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)

        val reports =
            if (authenticatedUser.isAdmin()) {
                conclusionReportService.getAllReports()
            } else {
                when (val result = conclusionReportService.getReportsByUserId(authenticatedUser.userId)) {
                    is Success -> result.value
                    is Failure -> return result.value.toProblemResponse()
                }
            }

        return ResponseEntity.ok(reports.map { it.toOutputDto() })
    }

    @GetMapping(Uris.Analyses.REPORTS)
    fun getReports(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (val result = conclusionReportService.getReportsByAnalysisId(analysisId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Analyses.REPORTS)
    fun createReport(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable analysisId: Int,
        @RequestBody(required = false) request: CreateReportRequestDto?,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeAnalysis(currentUser, analysisId)) {
            is AnalysisAccessResult.Authorized -> Unit
            is AnalysisAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                conclusionReportService.createReport(
                    analysisId = analysisId,
                    imageEvidenceIds = request?.imageEvidenceIds ?: emptyList(),
                    conclusion = request?.conclusion,
                    description = request?.description,
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/reports/${result.value.report.reportId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Reports.GET_BY_ID)
    fun getReport(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable reportId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeReport(currentUser, reportId)) {
            is ReportAccessResult.Authorized -> Unit
            is ReportAccessResult.Rejected -> return access.response
        }

        return when (val result = conclusionReportService.getReportDetailsById(reportId)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Reports.UPDATE_BY_ID)
    fun updateReport(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable reportId: Int,
        @RequestBody request: UpdateReportRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeReport(currentUser, reportId)) {
            is ReportAccessResult.Authorized -> Unit
            is ReportAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                conclusionReportService.updateReport(
                    reportId = reportId,
                    imageEvidenceIds = request.imageEvidenceIds,
                    conclusion = request.conclusion,
                    description = request.description,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Reports.DELETE_BY_ID)
    fun deleteReport(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable reportId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeReport(currentUser, reportId)) {
            is ReportAccessResult.Authorized -> Unit
            is ReportAccessResult.Rejected -> return access.response
        }

        return when (val result = conclusionReportService.deleteReport(reportId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
