package pt.isel.repositoryjdbi

import pt.isel.domain.Token
import pt.isel.domain.User
import java.time.Instant



data class UserAndTokenModel(
    val user_id: Int,
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
                User(user_id, username, email, password_hash),
                Token(
                    token_id,
                    user_id,
                    token,
                    Instant.ofEpochSecond(created_at),
                    Instant.ofEpochSecond(expires_at),
                    revoked
                ),
            )
}
