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
        caseId: Int,
        imageEvidenceIds: List<Int>,
        conclusion: String?,
        description: String?,
    ): Report {
        val report =
            handle.createUpdate(
                """
                INSERT INTO report (analysis_id, case_id, conclusion, report_description)
                VALUES (:analysis_id, :case_id, :conclusion, :report_description)
                """,
            )
                .bind("analysis_id", analysisId)
                .bind("case_id", caseId)
                .bind("conclusion", conclusion)
                .bind("report_description", description)
                .executeAndReturnGeneratedKeys(
                    "report_id",
                    "analysis_id",
                    "case_id",
                    "generated_at",
                    "conclusion",
                    "report_description",
                )
                .map(ReportMapper())
                .one()

        insertReportImages(report.reportId, imageEvidenceIds)
        return report
    }

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

    override fun findAllReports(): List<Report> =
        handle.createQuery("SELECT * FROM report ORDER BY generated_at DESC, report_id DESC")
            .map(ReportMapper())
            .list()

    override fun findReportsByUserId(userId: Int): List<Report> =
        handle.createQuery(
            """
            SELECT r.* FROM report r
            JOIN accident_case c ON r.case_id = c.case_id
            WHERE c.user_id = :user_id
            ORDER BY r.generated_at DESC, r.report_id DESC
            """,
        )
            .bind("user_id", userId)
            .map(ReportMapper())
            .list()

    override fun updateReport(
        reportId: Int,
        imageEvidenceIds: List<Int>?,
        conclusion: String?,
        description: String?,
    ): Report? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE report
                SET conclusion = :conclusion,
                    report_description = :report_description
                WHERE report_id = :report_id
                """,
            )
                .bind("report_id", reportId)
                .bind("conclusion", conclusion)
                .bind("report_description", description)
                .execute()

        if (rowsUpdated == 0) return null

        if (imageEvidenceIds != null) {
            handle.createUpdate("DELETE FROM report_image WHERE report_id = :report_id")
                .bind("report_id", reportId)
                .execute()
            insertReportImages(reportId, imageEvidenceIds)
        }

        return findReportById(reportId)
    }

    override fun deleteReportById(reportId: Int): Int =
        handle.createUpdate("DELETE FROM report WHERE report_id = :report_id")
            .bind("report_id", reportId)
            .execute()

    override fun findReportImageEvidenceIds(reportId: Int): List<Int> =
        handle.createQuery("SELECT image_evidence_id FROM report_image WHERE report_id = :report_id")
            .bind("report_id", reportId)
            .mapTo(Int::class.java)
            .list()

    private fun insertReportImages(
        reportId: Int,
        imageEvidenceIds: List<Int>,
    ) {
        imageEvidenceIds.distinct().forEach { imageEvidenceId ->
            handle.createUpdate(
                """
                INSERT INTO report_image (report_id, image_evidence_id)
                VALUES (:report_id, :image_evidence_id)
                """,
            )
                .bind("report_id", reportId)
                .bind("image_evidence_id", imageEvidenceId)
                .execute()
        }
    }

    private fun findAnalysisConclusionById(conclusionId: Int): AnalysisConclusion? =
        handle.createQuery("SELECT * FROM analysis_conclusion WHERE conclusion_id = :conclusion_id")
            .bind("conclusion_id", conclusionId)
            .map(AnalysisConclusionMapper())
            .singleOrNull()
}
