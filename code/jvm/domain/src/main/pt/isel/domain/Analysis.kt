package pt.isel.domain
import java.time.Instant


data class Analysis(
    val analysisId: Int,
    val caseId: Int,
    val analystId: Int,
    val createdAt: Instant
)