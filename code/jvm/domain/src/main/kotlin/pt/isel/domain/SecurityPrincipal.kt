package pt.isel.domain

data class SecurityPrincipal(
    val userId: Int,
    val username: String,
    val email: String,
    val role: UserRole,
) {
    fun isAdmin(): Boolean = role == UserRole.ADMIN
}

fun User.toSecurityPrincipal(): SecurityPrincipal =
    SecurityPrincipal(
        userId = userId,
        username = username,
        email = email,
        role = role,
    )
