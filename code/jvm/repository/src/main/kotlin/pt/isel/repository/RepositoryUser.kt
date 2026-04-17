package pt.isel.repository

import pt.isel.domain.User
import pt.isel.domain.Token
import java.time.Instant


/**
 * Repository interface for managing users, extends the generic Repository
 */
interface RepositoryUser : Repository<User> {
    fun createUser(
        email: String,
        password_hash: String,
    ): User

    fun getUserByUsername(username: String): User?

    fun createToken(
        token: Token,
        maxTokens: Int,
    )

    fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Int

}
