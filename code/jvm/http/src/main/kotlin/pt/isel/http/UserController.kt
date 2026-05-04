package pt.isel.http

import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RequestHeader
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.User
import pt.isel.http.dto.CreateUserRequestDto
import pt.isel.http.dto.CurrentUserDto
import pt.isel.http.dto.LoginRequestDto
import pt.isel.http.dto.LoginResponseDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.UserOutputDto
import pt.isel.services.Either
import pt.isel.services.Failure
import pt.isel.services.Success
import pt.isel.services.TokenCreationError
import pt.isel.services.TokenError
import pt.isel.services.UserAuthService
import pt.isel.services.UserError

@RestController
class UserController(
    private val userService: UserAuthService,
) {
    @PostMapping(Uris.Users.CREATE)
    fun createUser(
        @RequestBody userRequest: CreateUserRequestDto,
    ): ResponseEntity<*> {
        val result: Either<UserError, User> =
            userService.createUser(userRequest.username, userRequest.email, userRequest.password)

        return when (result) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/users/${result.value.userId}")
                    .body(result.value.toOutputDto())

            is Failure ->
                when (result.value) {
                    is UserError.InvalidUsername ->
                        Problem.InvalidUsername.response(HttpStatus.BAD_REQUEST)

                    is UserError.InsecurePassword ->
                        Problem.InsecurePassword.response(HttpStatus.BAD_REQUEST)

                    is UserError.EmailAlreadyExists ->
                        Problem.EmailAlreadyExists.response(HttpStatus.CONFLICT)

                    is UserError.UsernameAlreadyExists ->
                        Problem.UsernameAlreadyExists.response(HttpStatus.CONFLICT)

                    is UserError.InvalidEmail ->
                        Problem.InvalidEmail.response(HttpStatus.BAD_REQUEST)

                    is UserError.UserNotFound ->
                        Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
                }
        }
    }

    @GetMapping(Uris.Users.LIST)
    fun getUsers(): ResponseEntity<List<UserOutputDto>> = ResponseEntity.ok(userService.getUsers().map { it.toOutputDto() })

    @GetMapping(Uris.Users.GET_BY_ID)
    fun getUserById(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        when (val result = userService.getUserById(id)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
        }

    @DeleteMapping(Uris.Users.DELETE_BY_ID)
    fun deleteUserById(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        when (userService.deleteUser(id)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
        }

    @PostMapping(Uris.Users.LOGIN)
    fun login(
        @RequestBody loginRequest: LoginRequestDto,
    ): ResponseEntity<*> =
        when (val result = userService.createToken(loginRequest.email, loginRequest.password)) {
            is Success -> ResponseEntity.ok(LoginResponseDto(result.value))
            is Failure ->
                when (result.value) {
                    is TokenCreationError.UserOrPasswordAreInvalid ->
                        Problem.InvalidCredentials.response(HttpStatus.UNAUTHORIZED)
                }
        }

    @GetMapping(Uris.Users.ME)
    fun getCurrentUser(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<*> {
        val rawToken =
            authorization?.bearerToken()
                ?: return Problem.InvalidToken.response(HttpStatus.UNAUTHORIZED)

        return when (val result = userService.getUserByToken(rawToken)) {
            is Success ->
                ResponseEntity.ok(
                    CurrentUserDto(
                        userId = result.value.userId,
                        username = result.value.username,
                        email = result.value.email,
                        token = rawToken,
                    ),
                )

            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Users.LOGOUT)
    fun logout(
        @RequestHeader(HttpHeaders.AUTHORIZATION, required = false) authorization: String?,
    ): ResponseEntity<*> {
        val rawToken =
            authorization?.bearerToken()
                ?: return Problem.InvalidToken.response(HttpStatus.UNAUTHORIZED)

        return if (userService.deleteToken(rawToken)) {
            ResponseEntity.noContent().build<Unit>()
        } else {
            Problem.InvalidToken.response(HttpStatus.UNAUTHORIZED)
        }
    }

    private fun User.toOutputDto() =
        UserOutputDto(
            userId = userId,
            username = username,
            email = email,
        )

    private fun String.bearerToken(): String? {
        val prefix = "Bearer "
        return takeIf { it.startsWith(prefix, ignoreCase = true) }
            ?.substring(prefix.length)
            ?.trim()
            ?.takeIf { it.isNotBlank() }
    }

    private fun TokenError.toProblemResponse(): ResponseEntity<Any> =
        when (this) {
            is TokenError.InvalidToken -> Problem.InvalidToken.response(HttpStatus.UNAUTHORIZED)
            is TokenError.ExpiredToken -> Problem.ExpiredToken.response(HttpStatus.UNAUTHORIZED)
        }
}
