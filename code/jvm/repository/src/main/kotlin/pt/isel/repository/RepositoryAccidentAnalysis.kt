package pt.isel.repository

import pt.isel.domain.Analysis
import pt.isel.domain.AnalysisImage

interface RepositoryAccidentAnalysis {
    fun createAnalysis(
        caseId: Int,
        analystId: Int,
    ): Analysis

    fun findAnalysisById(analysisId: Int): Analysis?

    fun findAnalysesByCaseId(caseId: Int): List<Analysis>

    fun deleteAnalysisById(analysisId: Int): Int

    fun createAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
        purpose: String?,
    ): AnalysisImage

    fun findAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
    ): AnalysisImage?

    fun findAnalysisImagesByAnalysisId(analysisId: Int): List<AnalysisImage>

    fun updateAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
        purpose: String?,
    ): AnalysisImage?

    fun deleteAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
    ): Int
}
