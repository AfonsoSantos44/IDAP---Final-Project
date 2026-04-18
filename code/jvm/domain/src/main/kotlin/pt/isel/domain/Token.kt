package pt.isel.domain

import java.time.Instant

data class Token(
    val user_id: Int,
    val token_hash: String,
    val created_at: Instant,
    val expires_at: Instant,
    val revoked: Boolean,
)
