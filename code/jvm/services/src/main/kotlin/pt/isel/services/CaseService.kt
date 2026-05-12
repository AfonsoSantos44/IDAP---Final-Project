package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.AccidentCase
import pt.isel.repository.TransactionManager

sealed class CaseError {
    data object UserNotFound : CaseError()

    data object CaseNotFound : CaseError()

    data object InvalidCaseStatus : CaseError()

    data object InvalidCaseDescription : CaseError()
}

@Service
class CaseService(
    private val transactionManager: TransactionManager,
) {
    companion object {
        private const val DEFAULT_CASE_STATUS = "open"
        private const val MAX_CASE_STATUS_LENGTH = 50
        private const val MAX_DESCRIPTION_LENGTH = 5000
    }

    fun createCase(
        userId: Int,
        description: String?,
        status: String?,
    ): Either<CaseError, AccidentCase> {
        if (!isValidStatus(status)) return failure(CaseError.InvalidCaseStatus)
        if (!isValidDescription(description)) return failure(CaseError.InvalidCaseDescription)

        val normalizedStatus = normalizeStatus(status) ?: DEFAULT_CASE_STATUS
        val normalizedDescription = normalizeDescription(description)

        return transactionManager.run {
            repoUsers.findById(userId) ?: return@run failure(CaseError.UserNotFound)

            success(
                repoCases.createCase(
                    userId = userId,
                    caseStatus = normalizedStatus,
                    accidentDescription = normalizedDescription,
                ),
            )
        }
    }

    fun getCases(): List<AccidentCase> =
        transactionManager.run {
            repoCases.findAll()
        }

    fun getCaseById(caseId: Int): Either<CaseError, AccidentCase> =
        transactionManager.run {
            repoCases.findById(caseId)?.let { success(it) } ?: failure(CaseError.CaseNotFound)
        }

    fun getCasesByUserId(userId: Int): Either<CaseError, List<AccidentCase>> =
        transactionManager.run {
            repoUsers.findById(userId) ?: return@run failure(CaseError.UserNotFound)
            success(repoCases.findByUserId(userId))
        }

    fun updateCase(
        caseId: Int,
        description: String?,
        status: String?,
    ): Either<CaseError, AccidentCase> {
        if (!isValidStatus(status)) return failure(CaseError.InvalidCaseStatus)
        if (!isValidDescription(description)) return failure(CaseError.InvalidCaseDescription)

        val normalizedStatus = normalizeStatus(status)
        val normalizedDescription = normalizeDescription(description)

        return transactionManager.run {
            val currentCase = repoCases.findById(caseId) ?: return@run failure(CaseError.CaseNotFound)

            success(
                repoCases.updateCase(
                    caseId = caseId,
                    caseStatus = normalizedStatus ?: currentCase.caseStatus,
                    accidentDescription =
                        if (description == null) {
                            currentCase.accidentDescription
                        } else {
                            normalizedDescription
                        },
                ) ?: currentCase,
            )
        }
    }

    fun deleteCase(caseId: Int): Either<CaseError, Unit> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(CaseError.CaseNotFound)
            repoCases.deleteById(caseId)
            success(Unit)
        }

    private fun normalizeStatus(status: String?): String? = status?.trim()?.lowercase()

    private fun isValidStatus(status: String?): Boolean {
        if (status == null) return true

        val normalizedStatus = status.trim()
        return normalizedStatus.isNotEmpty() && normalizedStatus.length <= MAX_CASE_STATUS_LENGTH
    }

    private fun normalizeDescription(description: String?): String? = description?.trim()?.ifEmpty { null }

    private fun isValidDescription(description: String?): Boolean =
        description == null || description.trim().length <= MAX_DESCRIPTION_LENGTH
}
