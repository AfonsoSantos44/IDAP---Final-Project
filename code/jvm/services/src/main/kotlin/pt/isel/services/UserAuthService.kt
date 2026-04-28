package pt.isel.services

import jakarta.inject.Named
import pt.isel.domain.User

sealed class UserError {
    data object InsecurePassword : UserError()

    data object EmailAlreadyExists : UserError()

    data object UsernameAlreadyExists : UserError()

    data object InvalidEmail : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}

@Named
class UserAuthService() {
    fun createUser(
        username: String,
        email: String,
        password: String,
    ): Either<UserError, User> {
        TODO()
    }
}
