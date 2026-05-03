package pt.isel.repositoryjdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.domain.Token
import java.sql.ResultSet

class TokenMapper : RowMapper<Token> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Token =
        Token(
            userId = rs.getInt("user_id"),
            tokenHash = rs.getString("token_hash"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
            expiresAt = rs.getTimestamp("expires_at").toInstant(),
            revoked = rs.getBoolean("revoked"),
        )
}
