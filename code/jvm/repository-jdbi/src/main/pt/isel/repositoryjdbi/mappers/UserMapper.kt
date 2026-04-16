package pt.isel.repositoryjdbi.mappers


class UserMapper : RowMapper<User> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext?,
    ): User =
        User(
            user_id = rs.getInt("user_id"),
            username = rs.getString("username"),
            email = rs.getString("email"),
            password_hash = rs.getString("password_hash"),
            created_at = rs.getTimestamp("created_at"),
        )
}
