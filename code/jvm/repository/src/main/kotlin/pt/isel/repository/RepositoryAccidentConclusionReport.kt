package pt.isel.repository

import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.Report

interface RepositoryAccidentConclusionReport {
    fun createAnalysisConclusion(
        analysisId: Int,
        compatibilityResult: String,
        justification: String,
    ): AnalysisConclusion

    fun findAnalysisConclusionByAnalysisId(analysisId: Int): AnalysisConclusion?

    fun updateAnalysisConclusion(
        conclusionId: Int,
        compatibilityResult: String,
        justification: String,
    ): AnalysisConclusion?

    fun deleteAnalysisConclusionByAnalysisId(analysisId: Int): Int

    fun createReport(
        analysisId: Int,
        caseId: Int,
        imageEvidenceIds: List<Int>,
        conclusion: String?,
        description: String?,
    ): Report

    fun findReportById(reportId: Int): Report?

    fun findReportsByAnalysisId(analysisId: Int): List<Report>

    fun findAllReports(): List<Report>

    fun findReportsByUserId(userId: Int): List<Report>

    fun updateReport(
        reportId: Int,
        imageEvidenceIds: List<Int>?,
        conclusion: String?,
        description: String?,
    ): Report?

    fun deleteReportById(reportId: Int): Int

    fun findReportImageEvidenceIds(reportId: Int): List<Int>
}
