package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.AccidentCase
import pt.isel.repository.RepositoryCase
import pt.isel.repositoryjdbi.mappers.AccidentCaseMapper

class RepositoryCaseJdbi(private val handle: Handle) : RepositoryCase {
    override fun createCase(
        userId: Int,
        caseStatus: String,
        accidentDescription: String?,
    ): AccidentCase =
        handle.createUpdate(
            """
            INSERT INTO accident_case (user_id, case_status, accident_description)
            VALUES (:user_id, :case_status, :accident_description)
            """,
        )
            .bind("user_id", userId)
            .bind("case_status", caseStatus)
            .bind("accident_description", accidentDescription)
            .executeAndReturnGeneratedKeys(
                "case_id",
                "user_id",
                "created_at",
                "case_status",
                "accident_description",
            )
            .map(AccidentCaseMapper())
            .one()

    override fun findByUserId(userId: Int): List<AccidentCase> =
        handle.createQuery(
            """
            SELECT *
            FROM accident_case
            WHERE user_id = :user_id
            ORDER BY created_at DESC, case_id DESC
            """,
        )
            .bind("user_id", userId)
            .map(AccidentCaseMapper())
            .list()

    override fun updateCase(
        caseId: Int,
        caseStatus: String,
        accidentDescription: String?,
    ): AccidentCase? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE accident_case
                SET case_status = :case_status,
                    accident_description = :accident_description
                WHERE case_id = :case_id
                """,
            )
                .bind("case_id", caseId)
                .bind("case_status", caseStatus)
                .bind("accident_description", accidentDescription)
                .execute()

        return if (rowsUpdated == 0) null else findById(caseId)
    }

    override fun findById(id: Int): AccidentCase? =
        handle.createQuery("SELECT * FROM accident_case WHERE case_id = :case_id")
            .bind("case_id", id)
            .map(AccidentCaseMapper())
            .singleOrNull()

    override fun findAll(): List<AccidentCase> =
        handle.createQuery("SELECT * FROM accident_case ORDER BY created_at DESC, case_id DESC")
            .map(AccidentCaseMapper())
            .list()

    override fun save(entity: AccidentCase) {
        handle.createUpdate(
            """
            INSERT INTO accident_case (case_id, user_id, created_at, case_status, accident_description)
            VALUES (:case_id, :user_id, :created_at, :case_status, :accident_description)
            """,
        )
            .bind("case_id", entity.caseId)
            .bind("user_id", entity.userId)
            .bind("created_at", entity.createdAt)
            .bind("case_status", entity.caseStatus)
            .bind("accident_description", entity.accidentDescription)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle.createUpdate("DELETE FROM accident_case WHERE case_id = :case_id")
            .bind("case_id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM accident_case")
            .execute()
    }
}
