package pt.isel.domain
import java.time.Instant

data class Evidence(
    val evidenceId: Int,
    val caseId: Int,
    val evidenceType: String,
    val evidenceDescription: String,
    val uploadedBy: Int,
    val uploadedAt: Instant
)