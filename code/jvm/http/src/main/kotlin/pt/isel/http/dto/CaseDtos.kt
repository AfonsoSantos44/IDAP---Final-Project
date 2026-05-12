package pt.isel.http.dto

import java.time.Instant

data class CaseOutputDto(
    val caseId: Int,
    val userId: Int,
    val createdAt: Instant,
    val status: String,
    val description: String?,
)

data class CreateCaseRequestDto(
    val userId: Int,
    val description: String? = null,
    val status: String? = null,
)

data class UpdateCaseRequestDto(
    val description: String? = null,
    val status: String? = null,
)
