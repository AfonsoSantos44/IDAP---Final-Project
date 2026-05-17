package pt.isel.domain

enum class UserRole(val dbValue: String) {
    USER("user"),
    ADMIN("admin"),
    ;

    companion object {
        fun fromDbValue(value: String?): UserRole = entries.firstOrNull { it.dbValue.equals(value?.trim(), ignoreCase = true) } ?: USER
    }
}

data class User(
    val userId: Int,
    val username: String,
    val email: String,
    val passwordHash: String,
    val role: UserRole = UserRole.USER,
)
