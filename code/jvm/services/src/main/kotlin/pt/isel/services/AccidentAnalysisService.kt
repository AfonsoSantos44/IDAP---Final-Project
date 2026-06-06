package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.Analysis
import pt.isel.domain.AnalysisImage
import pt.isel.repository.TransactionManager

@Service
class AccidentAnalysisService(
    private val transactionManager: TransactionManager,
) {
    fun createAnalysis(
        caseId: Int,
        analystId: Int,
    ): Either<AccidentDataError, Analysis> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            repoUsers.findById(analystId) ?: return@run failure(AccidentDataError.UserNotFound)

            success(repoAccidentAnalysis.createAnalysis(caseId, analystId))
        }

    fun getAnalysesByCaseId(caseId: Int): Either<AccidentDataError, List<Analysis>> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            success(repoAccidentAnalysis.findAnalysesByCaseId(caseId))
        }

    fun getAnalysisById(analysisId: Int): Either<AccidentDataError, Analysis> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId)?.let { success(it) }
                ?: failure(AccidentDataError.AnalysisNotFound)
        }

    fun deleteAnalysis(analysisId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            repoAccidentAnalysis.deleteAnalysisById(analysisId)
            success(Unit)
        }

    fun upsertAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
        purpose: String?,
    ): Either<AccidentDataError, AnalysisImage> {
        if (!isValidOptionalText(purpose, MAX_ANALYSIS_IMAGE_PURPOSE)) {
            return failure(AccidentDataError.InvalidAccidentData)
        }

        return transactionManager.run {
            val analysis =
                repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            val evidence =
                repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)

            if (evidence.caseId != analysis.caseId) return@run failure(AccidentDataError.RelatedResourceMismatch)

            val currentImage = repoAccidentAnalysis.findAnalysisImage(analysisId, evidenceId)
            success(
                if (currentImage == null) {
                    repoAccidentAnalysis.createAnalysisImage(
                        analysisId = analysisId,
                        evidenceId = evidenceId,
                        purpose = normalizeOptionalText(purpose),
                    )
                } else {
                    repoAccidentAnalysis.updateAnalysisImage(
                        analysisId = analysisId,
                        evidenceId = evidenceId,
                        purpose = normalizeOptionalText(purpose),
                    ) ?: currentImage
                },
            )
        }
    }

    fun getAnalysisImagesByAnalysisId(analysisId: Int): Either<AccidentDataError, List<AnalysisImage>> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            success(repoAccidentAnalysis.findAnalysisImagesByAnalysisId(analysisId))
        }

    fun deleteAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
    ): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            if (repoAccidentAnalysis.deleteAnalysisImage(analysisId, evidenceId) == 0) {
                return@run failure(AccidentDataError.AnalysisImageNotFound)
            }
            success(Unit)
        }
}
