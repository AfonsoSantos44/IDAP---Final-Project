package pt.isel.domain

data class DamageComparison(
    val comparisonId: Int,
    val analysisId: Int,
    val damageSourceId: Int,
    val damageTargetId: Int,
    val compatibilityStatus: String,
    val notes: String?,
)
