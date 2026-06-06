package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.DamageComparison
import pt.isel.repository.RepositoryAccidentDamageComparison
import pt.isel.repositoryjdbi.mappers.DamageComparisonMapper

class RepositoryAccidentDamageComparisonJdbi(
    private val handle: Handle,
) : RepositoryAccidentDamageComparison {
    override fun createDamageComparison(
        analysisId: Int,
        damageSourceId: Int,
        damageTargetId: Int,
        compatibilityStatus: String,
        notes: String?,
    ): DamageComparison =
        handle.createUpdate(
            """
            INSERT INTO damage_comparison (
                analysis_id,
                damage_source_id,
                damage_target_id,
                compatibility_status,
                notes
            )
            VALUES (
                :analysis_id,
                :damage_source_id,
                :damage_target_id,
                :compatibility_status,
                :notes
            )
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("damage_source_id", damageSourceId)
            .bind("damage_target_id", damageTargetId)
            .bind("compatibility_status", compatibilityStatus)
            .bind("notes", notes)
            .executeAndReturnGeneratedKeys(
                "comparison_id",
                "analysis_id",
                "damage_source_id",
                "damage_target_id",
                "compatibility_status",
                "notes",
            )
            .map(DamageComparisonMapper())
            .one()

    override fun findDamageComparisonById(comparisonId: Int): DamageComparison? =
        handle.createQuery("SELECT * FROM damage_comparison WHERE comparison_id = :comparison_id")
            .bind("comparison_id", comparisonId)
            .map(DamageComparisonMapper())
            .singleOrNull()

    override fun findDamageComparisonsByAnalysisId(analysisId: Int): List<DamageComparison> =
        handle.createQuery(
            "SELECT * FROM damage_comparison WHERE analysis_id = :analysis_id ORDER BY comparison_id ASC",
        )
            .bind("analysis_id", analysisId)
            .map(DamageComparisonMapper())
            .list()

    override fun updateDamageComparison(
        comparisonId: Int,
        damageSourceId: Int,
        damageTargetId: Int,
        compatibilityStatus: String,
        notes: String?,
    ): DamageComparison? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE damage_comparison
                SET damage_source_id = :damage_source_id,
                    damage_target_id = :damage_target_id,
                    compatibility_status = :compatibility_status,
                    notes = :notes
                WHERE comparison_id = :comparison_id
                """,
            )
                .bind("comparison_id", comparisonId)
                .bind("damage_source_id", damageSourceId)
                .bind("damage_target_id", damageTargetId)
                .bind("compatibility_status", compatibilityStatus)
                .bind("notes", notes)
                .execute()

        return if (rowsUpdated == 0) null else findDamageComparisonById(comparisonId)
    }

    override fun deleteDamageComparisonById(comparisonId: Int): Int =
        handle.createUpdate("DELETE FROM damage_comparison WHERE comparison_id = :comparison_id")
            .bind("comparison_id", comparisonId)
            .execute()
}
