package pt.isel.repository

import pt.isel.domain.User
import pt.isel.domain.Token
import java.time.Instant


/**
 * Repository interface for managing users, extends the generic Repository
 */
interface RepositoryUser : Repository<User> {
    fun createUser(
        username: String,
        email: String,
        passwordHash: String,
    ): User

    fun getUserByEmail(email: String): User?

    fun getUserByUsername(username: String): User?

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun getTokenByHash(tokenHash: String): Token?

    fun deleteTokenByHash(tokenHash: String): Int

    fun deleteAllTokensByUserId(userId: Int): Int
}
