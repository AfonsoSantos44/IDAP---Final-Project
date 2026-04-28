package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import org.jdbi.v3.core.kotlin.mapTo
import pt.isel.domain.Token
import pt.isel.domain.User
import pt.isel.repository.RepositoryUser
import pt.isel.repositoryjdbi.mappers.UserMapper
import java.time.Instant

class RepositoryUserJdbi(private val handle: Handle) : RepositoryUser {
    override fun createUser(
        username: String,
        email: String,
        passwordHash: String,
    ): User {
        val id =
            handle.createUpdate("INSERT INTO users (username, email, password_hash) VALUES (:username, :email, :password_hash)")
                .bind("username", username)
                .bind("email", email)
                .bind("password_hash", passwordHash)
                .executeAndReturnGeneratedKeys().mapTo(Int::class.java)
                .one()
        return User(id, username, email, passwordHash)
    }

    override fun getUserByEmail(email: String): User? =
        handle.createQuery("SELECT * FROM users WHERE email = :email")
            .bind("email", email)
            .mapTo<User>()
            .singleOrNull()

    override fun getUserByUsername(username: String): User? =
        handle.createQuery("SELECT * FROM users WHERE username = :username")
            .bind("username", username)
            .mapTo<User>()
            .singleOrNull()

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        handle.createUpdate(
            """
            INSERT INTO tokens (user_id, token_hash, created_at, expires_at)
            VALUES (:user_id, :token_hash, :created_at, :expires_at)
            """,
        )
            .bind("user_id", token.user_id)
            .bind("token_hash", token.token_hash)
            .bind("created_at", token.created_at)
            .bind("expires_at", token.expires_at)
            .execute()
    }

    override fun getTokenByHash(tokenHash: String): Token? =
        handle.createQuery("SELECT * FROM tokens WHERE token_hash = :token_hash")
            .bind("token_hash", tokenHash)
            .mapTo<Token>()
            .singleOrNull()


    override fun deleteTokenByHash(tokenHash: String): Int =
        handle.createUpdate("DELETE FROM tokens WHERE token_hash = :token_hash")
            .bind("token_hash", tokenHash)
            .execute()

    override fun findById(id: Int): User? =
        handle.createQuery("SELECT * FROM users WHERE user_id = :user_id")
            .bind("user_id", id)
            .mapTo<User>()
            .singleOrNull()

    override fun findAll(): List<User> =
        handle.createQuery("SELECT * FROM users")
            .map(UserMapper())
            .list()

    override fun save(entity: User) {
        handle.createUpdate(
            "INSERT INTO users (user_id, username, email, password_hash) VALUES (:user_id, :username, :email, :password_hash)",
        )
            .bind("user_id", entity.userId)
            .bind("username", entity.username)
            .bind("email", entity.email)
            .bind("password_hash", entity.passwordHash)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle.createUpdate("DELETE FROM users WHERE user_id = :user_id")
            .bind("user_id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("DELETE FROM users")
            .execute()
    }
}
