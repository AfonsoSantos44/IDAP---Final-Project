package pt.isel.domain

import java.time.Instant

data class Token(
    val userId: Int,
    val tokenHash: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val revoked: Boolean,
)
