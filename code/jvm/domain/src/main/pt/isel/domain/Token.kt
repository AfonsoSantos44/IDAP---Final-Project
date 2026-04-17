package pt.isel.domain

import java.time.Instant

data class Token(
    val token_id: Int,
    val user_id: Int,
    val token: String,
    val created_at: Instant,
    val expires_at: Instant,
    val revoked: Boolean
)