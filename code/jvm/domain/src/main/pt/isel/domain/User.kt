package pt.isel.domain
import java.time.Instant

data class User(
    val userId: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
    val createdAt: Instant
)
