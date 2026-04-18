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
        password_hash: String,
    ): User {
        val id =
            handle.createUpdate("INSERT INTO dbo.users (username, email, password_hash) VALUES (:username, :email, :password_hash)")
                .bind("username", username)
                .bind("email", email)
                .bind("password_hash", password_hash)
                .executeAndReturnGeneratedKeys().mapTo(Int::class.java)
                .one()
        return User(id,username, email, password_hash)
    }

    override fun getUserByEmail(email: String): User? =
        handle.createQuery("select * from users where email = :email")
            .bind("email", email)
            .mapTo<User>()
            .singleOrNull()

    override fun getUserByUsername(username: String): User? =
        handle.createQuery("select * from users where username = :username")
            .bind("username", username)
            .mapTo<User>()
            .singleOrNull()

    override fun createToken(
        token: Token,
        maxTokens: Int,
    ) {
        handle.createUpdate(
            """
            insert into tokens (user_id, token_hash, created_at, expires_at)
            values (:user_id, :token_hash, :created_at, :expires_at)
            """,
        )
            .bind("user_id", token.user_id)
            .bind("token_hash", token.token_hash)
            .bind("created_at", token.created_at)
            .bind("expires_at", token.expires_at)
            .execute()
    }

    override fun getTokenByHash(token_hash: String): Token? =
        handle.createQuery("select * from tokens where token_hash = :token_hash")
            .bind("token_hash", token_hash)
            .mapTo<Token>()
            .singleOrNull()


    override fun updateTokenLastUsed(
        token: Token,
        now: Instant,
    ): Int =
        handle.createUpdate("update tokens set last_used_at = :last_used_at where token_hash = :token_hash")
            .bind("last_used_at", now)
            .bind("token_hash", token.token_hash)
            .execute()

    override fun deleteTokenByHash(token_hash: String): Int =
        handle.createUpdate("delete from tokens where token_hash = :token_hash")
            .bind("token_hash", token_hash)
            .execute()

    override fun findById(id: Int): User? =
        handle.createQuery("select * from users where user_id = :user_id")
            .bind("user_id", id)
            .mapTo<User>()
            .singleOrNull()

    override fun findAll(): List<User> =
        handle.createQuery("select * from users")
            .map(UserMapper())
            .list()

    override fun save(entity: User) {
        handle.createUpdate("insert into users (user_id, username, email, password_hash) values (:user_id, :username, :email, :password_hash)")
            .bind("user_id", entity.user_id)
            .bind("username", entity.username)
            .bind("email", entity.email)
            .bind("password_hash", entity.password_hash)
            .execute()
    }

    override fun deleteById(id: Int) {
        handle.createUpdate("delete from users where user_id = :user_id")
            .bind("user_id", id)
            .execute()
    }

    override fun clear() {
        handle.createUpdate("delete from users")
            .execute()
    }
}
