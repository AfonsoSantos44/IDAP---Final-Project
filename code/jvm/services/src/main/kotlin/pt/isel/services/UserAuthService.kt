package pt.isel.services

sealed class UserError {
    data object InsecurePassword : UserError()

    data object EmailAlreadyExists : UserError()

    data object UsernameAlreadyExists : UserError()

    data object InvalidEmail : UserError()
}

sealed class TokenCreationError {
    data object UserOrPasswordAreInvalid : TokenCreationError()
}
