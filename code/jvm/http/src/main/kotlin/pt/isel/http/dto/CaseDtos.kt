package pt.isel.http.dto

data class CaseOutputDto(
    val caseId: Int,
    val userId: Int,
    val createdAt: String,
    val status: String,
    val description: String?,
)

data class CreateCaseRequestDto(
    val userId: Int? = null,
    val description: String? = null,
    val status: String? = null,
)

data class UpdateCaseRequestDto(
    val description: String? = null,
    val status: String? = null,
)
