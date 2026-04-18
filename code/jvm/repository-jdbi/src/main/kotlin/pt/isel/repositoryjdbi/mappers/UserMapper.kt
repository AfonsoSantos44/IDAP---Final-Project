package pt.isel.repositoryjdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.domain.User
import java.sql.ResultSet

class UserMapper : RowMapper<User> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): User =
        User(
            user_id = rs.getInt("user_id"),
            username = rs.getString("username"),
            email = rs.getString("email"),
            password_hash = rs.getString("password_hash"),
        )
}
