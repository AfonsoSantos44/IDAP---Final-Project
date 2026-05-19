package pt.isel

import org.h2.jdbcx.JdbcDataSource
import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.jdbi.v3.core.kotlin.mapTo
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Assertions.fail
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import pt.isel.repositoryjdbi.TransactionManagerJdbi
import pt.isel.repositoryjdbi.mappers.AccidentCaseMapper
import pt.isel.repositoryjdbi.mappers.TokenMapper
import pt.isel.repositoryjdbi.mappers.UserMapper
import pt.isel.services.Either
import pt.isel.services.TokenError
import pt.isel.services.UserAuthService
import java.time.Clock
import java.time.Instant
import java.time.ZoneOffset
import java.util.UUID

class TokenPersistenceIntegrationTests {
    private lateinit var jdbi: Jdbi
    private lateinit var transactionManager: TransactionManagerJdbi

    @BeforeEach
    fun setUp() {
        val dataSource =
            JdbcDataSource().apply {
                setURL(
                    "jdbc:h2:mem:${UUID.randomUUID()};MODE=PostgreSQL;DATABASE_TO_LOWER=TRUE;DB_CLOSE_DELAY=-1",
                )
                user = "sa"
                password = ""
            }

        jdbi =
            Jdbi.create(dataSource)
                .installPlugin(KotlinPlugin())
                .registerRowMapper(UserMapper())
                .registerRowMapper(TokenMapper())
                .registerRowMapper(AccidentCaseMapper())
        transactionManager = TransactionManagerJdbi(jdbi)

        createUserAndTokenSchema()
    }

    @Test
    fun `deleted token is removed from the database and rejected by a new service instance`() {
        val service = newUserAuthService()
        val user =
            assertSuccess(
                service.createUser(
                    username = "token-owner",
                    email = "token-owner@example.com",
                    password = PASSWORD,
                ),
            )
        val session = assertSuccess(service.createToken(user.email, PASSWORD))

        assertEquals(user.userId, assertSuccess(service.getUserByToken(session.token)).userId)
        assertEquals(1, persistedTokenCount())

        assertTrue(service.deleteToken(session.token))
        assertEquals(0, persistedTokenCount())

        val serviceAfterRestart = newUserAuthService()
        assertEquals(
            TokenError.InvalidToken,
            assertFailure(serviceAfterRestart.getUserByToken(session.token)),
        )
        assertFalse(serviceAfterRestart.deleteToken(session.token))
    }

    private fun newUserAuthService() =
        UserAuthService(
            transactionManager = transactionManager,
            passwordEncoder = BCryptPasswordEncoder(),
            clock = Clock.fixed(Instant.parse("2026-05-19T12:00:00Z"), ZoneOffset.UTC),
        )

    private fun createUserAndTokenSchema() {
        jdbi.useHandle<Exception> { handle ->
            handle.execute(
                """
                CREATE TABLE users (
                    user_id SERIAL PRIMARY KEY,
                    username VARCHAR(100) NOT NULL UNIQUE,
                    email VARCHAR(255) NOT NULL UNIQUE,
                    password_hash VARCHAR(255) NOT NULL,
                    role VARCHAR(50) NOT NULL DEFAULT 'user' CHECK (role IN ('user', 'admin'))
                )
                """.trimIndent(),
            )
            handle.execute(
                """
                CREATE TABLE tokens (
                    user_id INT NOT NULL,
                    token_hash VARCHAR(255) NOT NULL UNIQUE,
                    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
                    expires_at TIMESTAMP NOT NULL,
                    revoked BOOLEAN NOT NULL DEFAULT FALSE,
                    FOREIGN KEY (user_id) REFERENCES users(user_id) ON DELETE CASCADE
                )
                """.trimIndent(),
            )
            handle.execute("CREATE INDEX idx_token_user ON tokens(user_id)")
        }
    }

    private fun persistedTokenCount(): Int =
        jdbi.withHandle<Int, Exception> { handle ->
            handle.createQuery("SELECT COUNT(*) FROM tokens")
                .mapTo<Int>()
                .one()
        }

    private fun <L, R> assertSuccess(result: Either<L, R>): R =
        when (result) {
            is Either.Right -> result.value
            is Either.Left -> fail("Expected success, got ${result.value}")
        }

    private fun <L, R> assertFailure(result: Either<L, R>): L =
        when (result) {
            is Either.Right -> fail("Expected failure, got ${result.value}")
            is Either.Left -> result.value
        }

    private companion object {
        const val PASSWORD = "StrongPass123"
    }
}