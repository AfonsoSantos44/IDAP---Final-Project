package pt.isel.repository

import pt.isel.domain.AccidentCase

interface RepositoryCase : Repository<AccidentCase> {
    fun createCase(
        userId: Int,
        caseStatus: String,
        accidentDescription: String?,
    ): AccidentCase

    fun findByUserId(userId: Int): List<AccidentCase>

    fun updateCase(
        caseId: Int,
        caseStatus: String,
        accidentDescription: String?,
    ): AccidentCase?
}