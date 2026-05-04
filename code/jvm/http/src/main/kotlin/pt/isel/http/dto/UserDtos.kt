package pt.isel.http.dto

data class UserOutputDto(
    val userId: Int,
    val username: String,
    val email: String,
)

data class CreateUserRequestDto(
    val username: String,
    val email: String,
    val password: String,
)

data class LoginRequestDto(
    val email: String,
    val password: String,
)

data class CurrentUserDto(
    val userId: Int,
    val username: String,
    val email: String,
)
