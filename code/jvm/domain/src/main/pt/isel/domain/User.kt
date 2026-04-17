package pt.isel.domain
import java.time.Instant

data class User(
    val user_id: Int,
    val username: String,
    val email: String,
    val password_hash: String,
    )
