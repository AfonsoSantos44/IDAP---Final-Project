package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.Report
import pt.isel.repository.TransactionManager

@Service
class AccidentConclusionReportService(
    private val transactionManager: TransactionManager,
    private val reportGenerator: AnalysisReportGenerator,
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
        filePath: String?,
    ): Either<AccidentDataError, Report> {
        if (!isValidOptionalText(filePath, MAX_FILE_PATH)) return failure(AccidentDataError.InvalidAccidentData)
        val normalizedRequestedFilePath = normalizeOptionalText(filePath)

        val snapshot =
            when (val result = loadReportSnapshot(analysisId)) {
                is Success -> result.value
                is Failure -> return result
            }

        val generatedFilePath =
            when (val result = reportGenerator.generate(snapshot, normalizedRequestedFilePath)) {
                is Success -> result.value
                is Failure -> return failure(AccidentDataError.ReportGenerationFailed)
            }

        if (!isValidOptionalText(generatedFilePath, MAX_FILE_PATH)) {
            return failure(AccidentDataError.ReportGenerationFailed)
        }

        return transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            success(repoAccidentConclusionReport.createReport(analysisId, generatedFilePath))
        }
    }

    fun getReportsByAnalysisId(analysisId: Int): Either<AccidentDataError, List<Report>> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            success(repoAccidentConclusionReport.findReportsByAnalysisId(analysisId))
        }

    fun getReportById(reportId: Int): Either<AccidentDataError, Report> =
        transactionManager.run {
            repoAccidentConclusionReport.findReportById(reportId)?.let { success(it) }
                ?: failure(AccidentDataError.ReportNotFound)
        }

    fun updateReport(
        reportId: Int,
        filePath: String?,
    ): Either<AccidentDataError, Report> {
        if (!isValidOptionalText(filePath, MAX_FILE_PATH)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val currentReport =
                repoAccidentConclusionReport.findReportById(reportId) ?: return@run failure(AccidentDataError.ReportNotFound)

            success(
                repoAccidentConclusionReport.updateReport(
                    reportId = reportId,
                    filePath = normalizeOptionalText(filePath) ?: currentReport.filePath,
                ) ?: currentReport,
            )
        }
    }

    fun deleteReport(reportId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentConclusionReport.findReportById(reportId) ?: return@run failure(AccidentDataError.ReportNotFound)
            repoAccidentConclusionReport.deleteReportById(reportId)
            success(Unit)
        }

    private fun loadReportSnapshot(analysisId: Int): Either<AccidentDataError, AnalysisReportSnapshot> =
        transactionManager.run {
            val analysis =
                repoAccidentAnalysis.findAnalysisById(analysisId)
                    ?: return@run failure(AccidentDataError.AnalysisNotFound)
            val accidentCase =
                repoCases.findById(analysis.caseId)
                    ?: return@run failure(AccidentDataError.CaseNotFound)
            val vehicles =
                repoAccidentVehicleDamage.findVehiclesByCaseId(accidentCase.caseId)
                    .map { vehicle ->
                        VehicleReportData(
                            vehicle = vehicle,
                            damages = repoAccidentVehicleDamage.findDamagesByVehicleId(vehicle.vehicleId),
                        )
                    }
            val evidence =
                repoAccidentEvidence.findEvidenceByCaseId(accidentCase.caseId)
                    .map { item ->
                        EvidenceReportData(
                            evidence = item,
                            image = repoAccidentEvidence.findImageEvidenceByEvidenceId(item.evidenceId),
                        )
                    }
            val evidenceById = evidence.associateBy { it.evidence.evidenceId }

            success(
                AnalysisReportSnapshot(
                    case = accidentCase,
                    weather = repoAccidentEnvironment.findWeatherByCaseId(accidentCase.caseId),
                    scene = repoAccidentEnvironment.findSceneByCaseId(accidentCase.caseId),
                    analysis = analysis,
                    vehicles = vehicles,
                    evidence = evidence,
                    analysisImages =
                        repoAccidentAnalysis.findAnalysisImagesByAnalysisId(analysisId)
                            .map { analysisImage ->
                                val evidenceItem = evidenceById[analysisImage.evidenceId]
                                AnalysisImageReportData(
                                    analysisImage = analysisImage,
                                    evidence = evidenceItem?.evidence,
                                    image = evidenceItem?.image,
                                )
                            },
                    measurements = repoAccidentMeasurement.findMeasurementsByAnalysisId(analysisId),
                    damageComparisons = repoAccidentDamageComparison.findDamageComparisonsByAnalysisId(analysisId),
                    conclusion = repoAccidentConclusionReport.findAnalysisConclusionByAnalysisId(analysisId),
                ),
            )
        }
}
