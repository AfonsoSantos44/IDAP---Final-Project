package pt.isel.domain
import java.time.Instant

data class AccidentCase(
    val caseId: Int,
    val userId: Int,
    val createdAt: Instant,
    val caseStatus: String,
    val accidentDescription: String? 
)