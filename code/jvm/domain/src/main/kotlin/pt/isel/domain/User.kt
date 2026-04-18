package pt.isel.domain

data class User(
    val user_id: Int,
    val username: String,
    val email: String,
    val password_hash: String,
)
