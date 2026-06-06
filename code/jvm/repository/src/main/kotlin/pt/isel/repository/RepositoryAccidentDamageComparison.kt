package pt.isel.repository

import pt.isel.domain.DamageComparison

interface RepositoryAccidentDamageComparison {
    fun createDamageComparison(
        analysisId: Int,
        damageSourceId: Int,
        damageTargetId: Int,
        compatibilityStatus: String,
        notes: String?,
    ): DamageComparison

    fun findDamageComparisonById(comparisonId: Int): DamageComparison?

    fun findDamageComparisonsByAnalysisId(analysisId: Int): List<DamageComparison>

    fun updateDamageComparison(
        comparisonId: Int,
        damageSourceId: Int,
        damageTargetId: Int,
        compatibilityStatus: String,
        notes: String?,
    ): DamageComparison?

    fun deleteDamageComparisonById(comparisonId: Int): Int
}
