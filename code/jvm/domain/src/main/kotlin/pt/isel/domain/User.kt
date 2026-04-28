package pt.isel.domain

data class User(
    val userId: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
)
