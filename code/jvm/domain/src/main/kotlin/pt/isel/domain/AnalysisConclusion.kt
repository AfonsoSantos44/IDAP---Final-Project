package pt.isel.domain

data class AnalysisConclusion(
    val conclusionId: Int,
    val analysisId: Int,
    val compatibilityResult: String,
    val justification: String,
)
