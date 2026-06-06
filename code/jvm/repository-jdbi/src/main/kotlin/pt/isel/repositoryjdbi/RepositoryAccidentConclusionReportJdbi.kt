package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.Report
import pt.isel.repository.RepositoryAccidentConclusionReport
import pt.isel.repositoryjdbi.mappers.AnalysisConclusionMapper
import pt.isel.repositoryjdbi.mappers.ReportMapper

class RepositoryAccidentConclusionReportJdbi(
    private val handle: Handle,
) : RepositoryAccidentConclusionReport {
    override fun createAnalysisConclusion(
        analysisId: Int,
        compatibilityResult: String,
        justification: String,
    ): AnalysisConclusion =
        handle.createUpdate(
            """
            INSERT INTO analysis_conclusion (analysis_id, compatibility_result, justification)
            VALUES (:analysis_id, :compatibility_result, :justification)
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("compatibility_result", compatibilityResult)
            .bind("justification", justification)
            .executeAndReturnGeneratedKeys(
                "conclusion_id",
                "analysis_id",
                "compatibility_result",
                "justification",
            )
            .map(AnalysisConclusionMapper())
            .one()

    override fun findAnalysisConclusionByAnalysisId(analysisId: Int): AnalysisConclusion? =
        handle.createQuery("SELECT * FROM analysis_conclusion WHERE analysis_id = :analysis_id")
            .bind("analysis_id", analysisId)
            .map(AnalysisConclusionMapper())
            .singleOrNull()

    override fun updateAnalysisConclusion(
        conclusionId: Int,
        compatibilityResult: String,
        justification: String,
    ): AnalysisConclusion? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE analysis_conclusion
                SET compatibility_result = :compatibility_result,
                    justification = :justification
                WHERE conclusion_id = :conclusion_id
                """,
            )
                .bind("conclusion_id", conclusionId)
                .bind("compatibility_result", compatibilityResult)
                .bind("justification", justification)
                .execute()

        return if (rowsUpdated == 0) null else findAnalysisConclusionById(conclusionId)
    }

    override fun deleteAnalysisConclusionByAnalysisId(analysisId: Int): Int =
        handle.createUpdate("DELETE FROM analysis_conclusion WHERE analysis_id = :analysis_id")
            .bind("analysis_id", analysisId)
            .execute()

    override fun createReport(
        analysisId: Int,
        filePath: String,
    ): Report =
        handle.createUpdate(
            """
            INSERT INTO report (analysis_id, file_path)
            VALUES (:analysis_id, :file_path)
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("file_path", filePath)
            .executeAndReturnGeneratedKeys(
                "report_id",
                "analysis_id",
                "generated_at",
                "file_path",
            )
            .map(ReportMapper())
            .one()

    override fun findReportById(reportId: Int): Report? =
        handle.createQuery("SELECT * FROM report WHERE report_id = :report_id")
            .bind("report_id", reportId)
            .map(ReportMapper())
            .singleOrNull()

    override fun findReportsByAnalysisId(analysisId: Int): List<Report> =
        handle.createQuery("SELECT * FROM report WHERE analysis_id = :analysis_id ORDER BY generated_at DESC, report_id DESC")
            .bind("analysis_id", analysisId)
            .map(ReportMapper())
            .list()

    override fun updateReport(
        reportId: Int,
        filePath: String,
    ): Report? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE report
                SET file_path = :file_path
                WHERE report_id = :report_id
                """,
            )
                .bind("report_id", reportId)
                .bind("file_path", filePath)
                .execute()

        return if (rowsUpdated == 0) null else findReportById(reportId)
    }

    override fun deleteReportById(reportId: Int): Int =
        handle.createUpdate("DELETE FROM report WHERE report_id = :report_id")
            .bind("report_id", reportId)
            .execute()

    private fun findAnalysisConclusionById(conclusionId: Int): AnalysisConclusion? =
        handle.createQuery("SELECT * FROM analysis_conclusion WHERE conclusion_id = :conclusion_id")
            .bind("conclusion_id", conclusionId)
            .map(AnalysisConclusionMapper())
            .singleOrNull()
}
