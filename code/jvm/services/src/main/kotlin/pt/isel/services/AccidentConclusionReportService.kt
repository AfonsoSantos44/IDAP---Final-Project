package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.ImageEvidence
import pt.isel.domain.Report
import pt.isel.domain.Vehicle
import pt.isel.repository.Transaction
import pt.isel.repository.TransactionManager

data class ReportDetails(
    val report: Report,
    val vehicles: List<Vehicle>,
    val images: List<ImageEvidence>,
)

@Service
class AccidentConclusionReportService(
    private val transactionManager: TransactionManager,
) {
    fun upsertAnalysisConclusion(
        analysisId: Int,
        compatibilityResult: String,
        justification: String,
    ): Either<AccidentDataError, AnalysisConclusion> {
        val normalizedCompatibilityResult =
            normalizeRequiredText(compatibilityResult, MAX_STATUS_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedJustification =
            normalizeRequiredText(justification, MAX_LONG_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            val currentConclusion = repoAccidentConclusionReport.findAnalysisConclusionByAnalysisId(analysisId)

            success(
                if (currentConclusion == null) {
                    repoAccidentConclusionReport.createAnalysisConclusion(
                        analysisId = analysisId,
                        compatibilityResult = normalizedCompatibilityResult,
                        justification = normalizedJustification,
                    )
                } else {
                    repoAccidentConclusionReport.updateAnalysisConclusion(
                        conclusionId = currentConclusion.conclusionId,
                        compatibilityResult = normalizedCompatibilityResult,
                        justification = normalizedJustification,
                    ) ?: currentConclusion
                },
            )
        }
    }

    fun getAnalysisConclusionByAnalysisId(analysisId: Int): Either<AccidentDataError, AnalysisConclusion> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            repoAccidentConclusionReport.findAnalysisConclusionByAnalysisId(analysisId)?.let { success(it) }
                ?: failure(AccidentDataError.AnalysisConclusionNotFound)
        }

    fun deleteAnalysisConclusion(analysisId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            if (repoAccidentConclusionReport.deleteAnalysisConclusionByAnalysisId(analysisId) == 0) {
                return@run failure(AccidentDataError.AnalysisConclusionNotFound)
            }
            success(Unit)
        }

    fun createReport(
        analysisId: Int,
        imageEvidenceIds: List<Int> = emptyList(),
        conclusion: String?,
        description: String?,
    ): Either<AccidentDataError, ReportDetails> {
        if (!isValidOptionalText(conclusion, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(description, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val analysis =
                repoAccidentAnalysis.findAnalysisById(analysisId)
                    ?: return@run failure(AccidentDataError.AnalysisNotFound)

            when (val validation = validateImageEvidenceIds(imageEvidenceIds, analysis.caseId)) {
                is Failure -> return@run validation
                is Success -> Unit
            }

            val report =
                repoAccidentConclusionReport.createReport(
                    analysisId = analysisId,
                    caseId = analysis.caseId,
                    imageEvidenceIds = imageEvidenceIds.distinct(),
                    conclusion = normalizeOptionalText(conclusion),
                    description = normalizeOptionalText(description),
                )
            success(buildReportDetails(report))
        }
    }

    fun getReportsByAnalysisId(analysisId: Int): Either<AccidentDataError, List<ReportDetails>> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            success(
                repoAccidentConclusionReport.findReportsByAnalysisId(analysisId).map { buildReportDetails(it) },
            )
        }

    fun getAllReports(): List<ReportDetails> =
        transactionManager.run {
            repoAccidentConclusionReport.findAllReports().map { buildReportDetails(it) }
        }

    fun getReportsByUserId(userId: Int): Either<AccidentDataError, List<ReportDetails>> =
        transactionManager.run {
            repoUsers.findById(userId) ?: return@run failure(AccidentDataError.UserNotFound)
            success(
                repoAccidentConclusionReport.findReportsByUserId(userId).map { buildReportDetails(it) },
            )
        }

    fun getReportById(reportId: Int): Either<AccidentDataError, Report> =
        transactionManager.run {
            repoAccidentConclusionReport.findReportById(reportId)?.let { success(it) }
                ?: failure(AccidentDataError.ReportNotFound)
        }

    fun getReportDetailsById(reportId: Int): Either<AccidentDataError, ReportDetails> =
        transactionManager.run {
            val report =
                repoAccidentConclusionReport.findReportById(reportId)
                    ?: return@run failure(AccidentDataError.ReportNotFound)
            success(buildReportDetails(report))
        }

    fun updateReport(
        reportId: Int,
        imageEvidenceIds: List<Int>?,
        conclusion: String?,
        description: String?,
    ): Either<AccidentDataError, ReportDetails> {
        if (!isValidOptionalText(conclusion, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(description, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val currentReport =
                repoAccidentConclusionReport.findReportById(reportId) ?: return@run failure(AccidentDataError.ReportNotFound)

            if (imageEvidenceIds != null) {
                when (val validation = validateImageEvidenceIds(imageEvidenceIds, currentReport.caseId)) {
                    is Failure -> return@run validation
                    is Success -> Unit
                }
            }

            val updatedReport =
                repoAccidentConclusionReport.updateReport(
                    reportId = reportId,
                    imageEvidenceIds = imageEvidenceIds?.distinct(),
                    conclusion = if (conclusion == null) currentReport.conclusion else normalizeOptionalText(conclusion),
                    description = if (description == null) currentReport.description else normalizeOptionalText(description),
                ) ?: currentReport

            success(buildReportDetails(updatedReport))
        }
    }

    fun deleteReport(reportId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentConclusionReport.findReportById(reportId) ?: return@run failure(AccidentDataError.ReportNotFound)
            repoAccidentConclusionReport.deleteReportById(reportId)
            success(Unit)
        }

    private fun Transaction.buildReportDetails(report: Report): ReportDetails =
        ReportDetails(
            report = report,
            vehicles = repoAccidentVehicleDamage.findVehiclesByCaseId(report.caseId),
            images =
                repoAccidentConclusionReport.findReportImageEvidenceIds(report.reportId)
                    .mapNotNull { repoAccidentEvidence.findImageEvidenceById(it) },
        )

    private fun Transaction.validateImageEvidenceIds(
        imageEvidenceIds: List<Int>,
        caseId: Int,
    ): Either<AccidentDataError, Unit> {
        imageEvidenceIds.forEach { imageEvidenceId ->
            val image =
                repoAccidentEvidence.findImageEvidenceById(imageEvidenceId)
                    ?: return failure(AccidentDataError.ImageEvidenceNotFound)
            val vehicle =
                repoAccidentVehicleDamage.findVehicleById(image.vehicleId)
                    ?: return failure(AccidentDataError.ImageEvidenceNotFound)
            if (vehicle.caseId != caseId) return failure(AccidentDataError.RelatedResourceMismatch)
        }
        return success(Unit)
    }
}
