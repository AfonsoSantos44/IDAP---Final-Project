package pt.isel.http

import org.springframework.http.ResponseEntity
import org.springframework.http.HttpStatus
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestMapping
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.User
import pt.isel.http.dto.CreateUserRequestDto
import pt.isel.http.dto.UserOutputDto
import pt.isel.repository.TransactionManager
import pt.isel.services.Either
import pt.isel.services.UserError
import Uris

// Controller for user-related endpoints

@RestController
@RequestMapping("/users")
class UserController(
    private val transactionManager: TransactionManager,
) {
    companion object {
        private const val TOKEN_COOKIE_NAME = "token"
        private const val COOKIE_PATH = "/"
        private const val COOKIE_MAX_AGE_SECONDS = 60 * 60 * 24 // 24 hours
    }

    @PostMapping(Uris.Users.CREATE)
    fun createUser(
        @RequestBody userRequest: CreateUserRequestDto,
    ): ResponseEntity<UserOutputDto> {
        val result: Either<UserError, User> =
            userService.createUser(userRequest.username, userRequest.email, userRequest.password)

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/users/${result.value.id}")
                    .build()

            is Failure ->
                when (result.value) {
                    is UserError.InsecurePassword ->
                        Problem.InsecurePassword.response(HttpStatus.BAD_REQUEST)

                    is UserError.EmailAlreadyExists ->
                        Problem.EmailAlreadyExists.response(HttpStatus.CONFLICT)

                    is UserError.UsernameAlreadyExists ->
                        Problem.UsernameAlreadyExists.response(HttpStatus.CONFLICT)

                    is UserError.InvalidEmail ->
                        Problem.InvalidEmail.response(HttpStatus.BAD_REQUEST)
                }
        }
    }
}
