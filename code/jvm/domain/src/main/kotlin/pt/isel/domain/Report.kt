package pt.isel.domain
import java.time.Instant

data class Report(
    val reportId: Int,
    val analysisId: Int,
    val caseId: Int,
    val generatedAt: Instant,
    val conclusion: String?,
    val description: String?,
)
