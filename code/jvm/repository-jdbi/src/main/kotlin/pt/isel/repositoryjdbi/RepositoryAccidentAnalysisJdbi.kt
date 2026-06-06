package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.Analysis
import pt.isel.domain.AnalysisImage
import pt.isel.repository.RepositoryAccidentAnalysis
import pt.isel.repositoryjdbi.mappers.AnalysisImageMapper
import pt.isel.repositoryjdbi.mappers.AnalysisMapper

class RepositoryAccidentAnalysisJdbi(
    private val handle: Handle,
) : RepositoryAccidentAnalysis {
    override fun createAnalysis(
        caseId: Int,
        analystId: Int,
    ): Analysis =
        handle.createUpdate(
            """
            INSERT INTO analysis (case_id, analyst_id)
            VALUES (:case_id, :analyst_id)
            """,
        )
            .bind("case_id", caseId)
            .bind("analyst_id", analystId)
            .executeAndReturnGeneratedKeys(
                "analysis_id",
                "case_id",
                "analyst_id",
                "created_at",
            )
            .map(AnalysisMapper())
            .one()

    override fun findAnalysisById(analysisId: Int): Analysis? =
        handle.createQuery("SELECT * FROM analysis WHERE analysis_id = :analysis_id")
            .bind("analysis_id", analysisId)
            .map(AnalysisMapper())
            .singleOrNull()

    override fun findAnalysesByCaseId(caseId: Int): List<Analysis> =
        handle.createQuery("SELECT * FROM analysis WHERE case_id = :case_id ORDER BY created_at DESC, analysis_id DESC")
            .bind("case_id", caseId)
            .map(AnalysisMapper())
            .list()

    override fun deleteAnalysisById(analysisId: Int): Int =
        handle.createUpdate("DELETE FROM analysis WHERE analysis_id = :analysis_id")
            .bind("analysis_id", analysisId)
            .execute()

    override fun createAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
        purpose: String?,
    ): AnalysisImage =
        handle.createUpdate(
            """
            INSERT INTO analysis_image (analysis_id, evidence_id, purpose)
            VALUES (:analysis_id, :evidence_id, :purpose)
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("evidence_id", evidenceId)
            .bind("purpose", purpose)
            .executeAndReturnGeneratedKeys(
                "analysis_id",
                "evidence_id",
                "purpose",
            )
            .map(AnalysisImageMapper())
            .one()

    override fun findAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
    ): AnalysisImage? =
        handle.createQuery(
            """
            SELECT *
            FROM analysis_image
            WHERE analysis_id = :analysis_id AND evidence_id = :evidence_id
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("evidence_id", evidenceId)
            .map(AnalysisImageMapper())
            .singleOrNull()

    override fun findAnalysisImagesByAnalysisId(analysisId: Int): List<AnalysisImage> =
        handle.createQuery("SELECT * FROM analysis_image WHERE analysis_id = :analysis_id ORDER BY evidence_id ASC")
            .bind("analysis_id", analysisId)
            .map(AnalysisImageMapper())
            .list()

    override fun updateAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
        purpose: String?,
    ): AnalysisImage? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE analysis_image
                SET purpose = :purpose
                WHERE analysis_id = :analysis_id AND evidence_id = :evidence_id
                """,
            )
                .bind("analysis_id", analysisId)
                .bind("evidence_id", evidenceId)
                .bind("purpose", purpose)
                .execute()

        return if (rowsUpdated == 0) null else findAnalysisImage(analysisId, evidenceId)
    }

    override fun deleteAnalysisImage(
        analysisId: Int,
        evidenceId: Int,
    ): Int =
        handle.createUpdate(
            """
            DELETE FROM analysis_image
            WHERE analysis_id = :analysis_id AND evidence_id = :evidence_id
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("evidence_id", evidenceId)
            .execute()
}
