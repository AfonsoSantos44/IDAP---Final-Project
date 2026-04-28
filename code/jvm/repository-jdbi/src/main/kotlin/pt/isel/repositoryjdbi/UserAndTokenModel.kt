package pt.isel.repositoryjdbi

import pt.isel.domain.Token
import pt.isel.domain.User
import java.time.Instant

data class UserAndTokenModel(
    val userId: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
    val token: String,
    val createdAt: Long,
    val expiresAt: Long,
    val revoked: Boolean,
) {
    val userAndToken: Pair<User, Token>
        get() =
            Pair(
                User(
                    userId,
                    username,
                    email,
                    passwordHash,
                ),
                Token(
                    userId,
                    token,
                    Instant.ofEpochSecond(createdAt),
                    Instant.ofEpochSecond(expiresAt),
                    revoked,
                ),
            )
}
