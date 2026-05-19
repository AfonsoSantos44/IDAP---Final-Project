package pt.isel.http

import org.springframework.beans.factory.annotation.Value
import org.springframework.http.HttpHeaders
import org.springframework.http.HttpStatus
import org.springframework.http.ResponseCookie
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.SecurityPrincipal
import pt.isel.domain.User
import pt.isel.http.dto.CreateUserRequestDto
import pt.isel.http.dto.CurrentUserDto
import pt.isel.http.dto.LoginRequestDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.UserOutputDto
import pt.isel.services.Either
import pt.isel.services.Failure
import pt.isel.services.Success
import pt.isel.services.TokenCreationError
import pt.isel.services.UserAuthService
import pt.isel.services.UserError

@RestController
class UserController(
    private val userService: UserAuthService,
    @Value("\${idap.session.cookie.secure:false}")
    private val secureSessionCookie: Boolean,
) {
    companion object {
        private const val SESSION_COOKIE_NAME = "idap_session"
        private const val SESSION_MAX_AGE_SECONDS = 60L * 60 * 24
    }

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
    fun getUsers(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
    ): ResponseEntity<*> {
        currentUser.requireAdmin()?.let { return it }

        return ResponseEntity.ok(userService.getUsers().map { it.toOutputDto() })
    }

    @GetMapping(Uris.Users.GET_BY_ID)
    fun getUserById(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        val authenticatedUser = currentUser ?: return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)
        if (!authenticatedUser.canAccessUser(id)) {
            return Problem.AccessDenied.response(HttpStatus.FORBIDDEN)
        }

        return when (val result = userService.getUserById(id)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
        }
    }

    @DeleteMapping(Uris.Users.DELETE_BY_ID)
    fun deleteUserById(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        currentUser.requireAdmin()?.let { return it }

        return when (userService.deleteUser(id)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
        }
    }

    @PostMapping(Uris.Users.LOGIN)
    fun login(
        @RequestBody loginRequest: LoginRequestDto,
    ): ResponseEntity<*> =
        when (val result = userService.createToken(loginRequest.email, loginRequest.password)) {
            is Success ->
                ResponseEntity
                    .ok()
                    .header(HttpHeaders.SET_COOKIE, createSessionCookie(result.value.token).toString())
                    .body(result.value.user.toCurrentUserDto())

            is Failure ->
                when (result.value) {
                    is TokenCreationError.UserOrPasswordAreInvalid ->
                        Problem.InvalidCredentials.response(HttpStatus.UNAUTHORIZED)
                }
        }

    @GetMapping(Uris.Users.ME)
    fun getCurrentUser(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
    ): ResponseEntity<*> {
        val authenticatedUser = currentUser ?: return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)

        return ResponseEntity.ok(authenticatedUser.toCurrentUserDto())
    }

    @PostMapping(Uris.Users.LOGOUT)
    fun logout(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
    ): ResponseEntity<*> {
        if (currentUser == null || rawToken.isNullOrBlank()) {
            return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)
        }

        return if (userService.deleteToken(rawToken)) {
            ResponseEntity
                .noContent()
                .header(HttpHeaders.SET_COOKIE, clearSessionCookie().toString())
                .build<Unit>()
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

    private fun User.toCurrentUserDto() =
        CurrentUserDto(
            userId = userId,
            username = username,
            email = email,
        )

    private fun SecurityPrincipal.toCurrentUserDto() =
        CurrentUserDto(
            userId = userId,
            username = username,
            email = email,
        )

    private fun createSessionCookie(rawToken: String): ResponseCookie =
        ResponseCookie
            .from(SESSION_COOKIE_NAME, rawToken)
            .httpOnly(true)
            .secure(secureSessionCookie)
            .sameSite("Lax")
            .path("/")
            .maxAge(SESSION_MAX_AGE_SECONDS)
            .build()

    private fun clearSessionCookie(): ResponseCookie =
        ResponseCookie
            .from(SESSION_COOKIE_NAME, "")
            .httpOnly(true)
            .secure(secureSessionCookie)
            .sameSite("Lax")
            .path("/")
            .maxAge(0)
            .build()

    private fun SecurityPrincipal.canAccessUser(userId: Int): Boolean = isAdmin() || this.userId == userId

    private fun SecurityPrincipal?.requireAdmin(): ResponseEntity<Any>? =
        when {
            this == null -> Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)
            !isAdmin() -> Problem.AccessDenied.response(HttpStatus.FORBIDDEN)
            else -> null
        }
}
