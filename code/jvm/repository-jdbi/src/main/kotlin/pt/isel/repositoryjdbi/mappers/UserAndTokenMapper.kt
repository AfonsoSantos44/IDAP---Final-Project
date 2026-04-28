package pt.isel.repositoryjdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.repositoryjdbi.UserAndTokenModel
import java.sql.ResultSet

class UserAndTokenMapper : RowMapper<UserAndTokenModel> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): UserAndTokenModel {
        return UserAndTokenModel(
            userId = rs.getInt("user_id"),
            username = rs.getString("username"),
            email = rs.getString("email"),
            passwordHash = rs.getString("password_hash"),
            token = rs.getString("token"),
            createdAt = rs.getTimestamp("created_at").time / 1000,
            expiresAt = rs.getTimestamp("expires_at").time / 1000,
            revoked = rs.getBoolean("revoked"),
        )
    }
}
