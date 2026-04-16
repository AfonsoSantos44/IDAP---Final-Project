package pt.isel.repositoryjdbi

data class UserAndTokenModel(
    val userId: Int,
    val username: String,
    val email: String,
    val password_hash: String,
    val token_id : Int,
    val token : String,
    val created_at: Long,
    val expires_at: Long,
    val revoked: Boolean
) {
    val userAndToken: Pair<User, Token>
        get() =
            Pair(
                User(userId, username, email, password_hash),
                Token(
                    token_id,
                    user_id,
                    token,
                    Instant.ofEpochSecond(createdAt),
                    Instant.ofEpochSecond(expires_at),
                    revoked
                ),
            )
}
