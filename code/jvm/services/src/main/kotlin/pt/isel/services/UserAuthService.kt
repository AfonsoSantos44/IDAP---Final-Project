package pt.isel.services

import org.springframework.security.crypto.password.PasswordEncoder
import org.springframework.stereotype.Service
import pt.isel.domain.Token
import pt.isel.domain.User
import pt.isel.repository.TransactionManager
import java.security.MessageDigest
import java.security.SecureRandom
import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.Base64

sealed class UserError {
    data object InvalidUsername : UserError()

    data object InsecurePassword : UserError()

    data object EmailAlreadyExists : UserError()

    data object UsernameAlreadyExists : UserError()

    data object InvalidEmail : UserError()

    data object UserNotFound : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}

sealed class TokenError {
    data object InvalidToken : TokenError()

    data object ExpiredToken : TokenError()
}

@Service
class UserAuthService(
    private val transactionManager: TransactionManager,
    private val passwordEncoder: PasswordEncoder,
    private val clock: Clock = Clock.systemUTC(),
) {
    companion object {
        private const val MAX_TOKENS_PER_USER = 5
        private const val RAW_TOKEN_BYTES = 32
        private val tokenTtl: Duration = Duration.ofHours(24)
        private val emailRegex = Regex("^[A-Z0-9._%+-]+@[A-Z0-9.-]+\\.[A-Z]{2,}$", RegexOption.IGNORE_CASE)
        private val secureRandom = SecureRandom()
    }

    fun createUser(
        username: String,
        email: String,
        password: String,
    ): Either<UserError, User> {
        val normalizedUsername = username.trim()
        val normalizedEmail = email.trim().lowercase()

        if (!isValidUsername(normalizedUsername)) return failure(UserError.InvalidUsername)
        if (!isValidEmail(normalizedEmail)) return failure(UserError.InvalidEmail)
        if (!isSecurePassword(password)) return failure(UserError.InsecurePassword)

        return transactionManager.run {
            if (repoUsers.getUserByUsername(normalizedUsername) != null) {
                return@run failure(UserError.UsernameAlreadyExists)
            }
            if (repoUsers.getUserByEmail(normalizedEmail) != null) {
                return@run failure(UserError.EmailAlreadyExists)
            }

            success(
                repoUsers.createUser(
                    username = normalizedUsername,
                    email = normalizedEmail,
                    passwordHash = passwordEncoder.encode(password),
                ),
            )
        }
    }

    fun getUsers(): List<User> =
        transactionManager.run {
            repoUsers.findAll()
        }

    fun getUserById(id: Int): Either<UserError, User> =
        transactionManager.run {
            repoUsers.findById(id)?.let { success(it) } ?: failure(UserError.UserNotFound)
        }

    fun deleteUser(id: Int): Either<UserError, Unit> =
        transactionManager.run {
            repoUsers.findById(id) ?: return@run failure(UserError.UserNotFound)
            repoUsers.deleteById(id)
            success(Unit)
        }

    fun createToken(
        email: String,
        password: String,
    ): Either<TokenCreationError, String> {
        val normalizedEmail = email.trim().lowercase()

        return transactionManager.run {
            val user =
                repoUsers.getUserByEmail(normalizedEmail)
                    ?: return@run failure(TokenCreationError.UserOrPasswordAreInvalid)

            if (!passwordEncoder.matches(password, user.passwordHash)) {
                return@run failure(TokenCreationError.UserOrPasswordAreInvalid)
            }

            val rawToken = generateRawToken()
            val now = Instant.now(clock)
            repoUsers.createToken(
                Token(
                    userId = user.userId,
                    tokenHash = hashToken(rawToken),
                    createdAt = now,
                    expiresAt = now.plus(tokenTtl),
                    revoked = false,
                ),
                MAX_TOKENS_PER_USER,
            )
            success(rawToken)
        }
    }

    fun getUserByToken(rawToken: String): Either<TokenError, User> =
        transactionManager.run {
            val token =
                repoUsers.getTokenByHash(hashToken(rawToken))
                    ?: return@run failure(TokenError.InvalidToken)

            if (token.revoked) return@run failure(TokenError.InvalidToken)
            if (token.expiresAt.isBefore(Instant.now(clock))) return@run failure(TokenError.ExpiredToken)

            repoUsers.findById(token.userId)?.let { success(it) } ?: failure(TokenError.InvalidToken)
        }

    fun deleteToken(rawToken: String): Boolean =
        transactionManager.run {
            repoUsers.deleteTokenByHash(hashToken(rawToken)) > 0
        }

    private fun isValidUsername(username: String): Boolean =
        username.length in 3..100 && username.all { it.isLetterOrDigit() || it == '_' || it == '-' || it == '.' }

    private fun isValidEmail(email: String): Boolean = email.length <= 255 && emailRegex.matches(email)

    private fun isSecurePassword(password: String): Boolean =
        password.length >= 8 &&
            password.any { it.isUpperCase() } &&
            password.any { it.isLowerCase() } &&
            password.any { it.isDigit() }

    private fun generateRawToken(): String {
        val bytes = ByteArray(RAW_TOKEN_BYTES)
        secureRandom.nextBytes(bytes)
        return Base64.getUrlEncoder().withoutPadding().encodeToString(bytes)
    }

    private fun hashToken(rawToken: String): String {
        val digest = MessageDigest.getInstance("SHA-256").digest(rawToken.toByteArray(Charsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }
}
