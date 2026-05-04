package pt.isel.repositoryjdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.domain.AccidentCase
import java.sql.ResultSet

class AccidentCaseMapper : RowMapper<AccidentCase> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): AccidentCase =
        AccidentCase(
            caseId = rs.getInt("case_id"),
            userId = rs.getInt("user_id"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            caseStatus = rs.getString("case_status"),
            accidentDescription = rs.getString("accident_description"),
        )
}