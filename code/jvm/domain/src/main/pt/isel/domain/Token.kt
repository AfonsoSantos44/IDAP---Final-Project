package pt.isel.domain

import java.time.Instant

data class Token(
    val tokenId: Int,
    val userId: Int,
    val token: String,
    val createdAt: Instant,
    val expiresAt: Instant,
    val revoked: Boolean
)