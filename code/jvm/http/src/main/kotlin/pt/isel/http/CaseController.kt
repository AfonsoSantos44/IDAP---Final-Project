package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.web.bind.annotation.CookieValue
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.AccidentCase
import pt.isel.domain.User
import pt.isel.domain.UserRole
import pt.isel.http.dto.CaseOutputDto
import pt.isel.http.dto.CreateCaseRequestDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.UpdateCaseRequestDto
import pt.isel.services.CaseError
import pt.isel.services.CaseService
import pt.isel.services.Failure
import pt.isel.services.Success
import pt.isel.services.TokenError
import pt.isel.services.UserAuthService
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RestController
class CaseController(
    private val caseService: CaseService,
    private val userAuthService: UserAuthService,
) {
    companion object {
        private const val SESSION_COOKIE_NAME = "idap_session"

        private val createdAtFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
    }

    @GetMapping(Uris.Cases.LIST)
    fun getCases(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
    ): ResponseEntity<*> {
        if (rawToken.isNullOrBlank()) {
            return ResponseEntity.ok(caseService.getCases().map { it.toOutputDto() })
        }

        return when (val currentUser = userAuthService.getUserByToken(rawToken)) {
            is Success -> {
                val cases =
                    if (currentUser.value.isSystemAdmin()) {
                        caseService.getCases()
                    } else {
                        when (val result = caseService.getCasesByUserId(currentUser.value.userId)) {
                            is Success -> result.value
                            is Failure -> return result.value.toProblemResponse()
                        }
                    }

                ResponseEntity.ok(cases.map { it.toOutputDto() })
            }

            is Failure -> currentUser.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Cases.CREATE)
    fun createCase(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
        @RequestBody request: CreateCaseRequestDto,
    ): ResponseEntity<*> {
        val caseOwnerId =
            resolveCaseOwner(rawToken) ?: request.userId
                ?: return Problem.NoUserLoggedIn.response(HttpStatus.BAD_REQUEST)

        return when (
            val result =
                caseService.createCase(
                    userId = caseOwnerId,
                    description = request.description,
                    status = request.status,
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/cases/${result.value.caseId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Cases.GET_BY_ID)
    fun getCaseById(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        when (val result = getAuthorizedCase(rawToken, id)) {
            is CaseAccessResult.Authorized -> ResponseEntity.ok(result.case.toOutputDto())
            is CaseAccessResult.Rejected -> result.response
        }

    @GetMapping(Uris.Cases.LIST_BY_USER_ID)
    fun getCasesByUserId(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
        @PathVariable userId: Int,
    ): ResponseEntity<*> {
        val currentUser =
            when (val result = getAuthenticatedUser(rawToken)) {
                is UserAccessResult.Authenticated -> result.user
                is UserAccessResult.Rejected -> return result.response
            }

        if (!currentUser.isSystemAdmin() && currentUser.userId != userId) {
            return Problem.CaseAccessDenied.response(HttpStatus.FORBIDDEN)
        }

        return when (val result = caseService.getCasesByUserId(userId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Cases.UPDATE_BY_ID)
    fun updateCase(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
        @PathVariable id: Int,
        @RequestBody request: UpdateCaseRequestDto,
    ): ResponseEntity<*> {
        when (val accessResult = getAuthorizedCase(rawToken, id)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return accessResult.response
        }

        return when (val result = caseService.updateCase(id, request.description, request.status)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Cases.DELETE_BY_ID)
    fun deleteCaseById(
        @CookieValue(name = SESSION_COOKIE_NAME, required = false) rawToken: String?,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        when (val accessResult = getAuthorizedCase(rawToken, id)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return accessResult.response
        }

        return when (val result = caseService.deleteCase(id)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }

    private fun AccidentCase.toOutputDto() =
        CaseOutputDto(
            caseId = caseId,
            userId = userId,
            createdAt = formatCreatedAt(createdAt),
            status = caseStatus,
            description = accidentDescription,
        )

    private fun formatCreatedAt(createdAt: Instant): String = createdAtFormatter.format(createdAt)

    private fun User.isSystemAdmin(): Boolean = role == UserRole.ADMIN

    private fun User.canAccess(accidentCase: AccidentCase): Boolean = isSystemAdmin() || userId == accidentCase.userId

    private fun getAuthorizedCase(
        rawToken: String?,
        caseId: Int,
    ): CaseAccessResult {
        val currentUser =
            when (val result = getAuthenticatedUser(rawToken)) {
                is UserAccessResult.Authenticated -> result.user
                is UserAccessResult.Rejected -> return CaseAccessResult.Rejected(result.response)
            }

        val accidentCase =
            when (val result = caseService.getCaseById(caseId)) {
                is Success -> result.value
                is Failure -> return CaseAccessResult.Rejected(result.value.toProblemResponse())
            }

        if (!currentUser.canAccess(accidentCase)) {
            return CaseAccessResult.Rejected(Problem.CaseAccessDenied.response(HttpStatus.FORBIDDEN))
        }

        return CaseAccessResult.Authorized(accidentCase)
    }

    private fun getAuthenticatedUser(rawToken: String?): UserAccessResult {
        if (rawToken.isNullOrBlank()) {
            return UserAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.BAD_REQUEST))
        }

        return when (val result = userAuthService.getUserByToken(rawToken)) {
            is Success -> UserAccessResult.Authenticated(result.value)
            is Failure -> UserAccessResult.Rejected(result.value.toProblemResponse())
        }
    }

    private fun resolveCaseOwner(rawToken: String?): Int? {
        if (rawToken.isNullOrBlank()) return null

        return when (val result = userAuthService.getUserByToken(rawToken)) {
            is Success -> result.value.userId
            is Failure -> null
        }
    }

    private fun CaseError.toProblemResponse(): ResponseEntity<Any> =
        when (this) {
            is CaseError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
            is CaseError.CaseNotFound -> Problem.CaseNotFound.response(HttpStatus.NOT_FOUND)
            is CaseError.InvalidCaseStatus -> Problem.InvalidCaseStatus.response(HttpStatus.BAD_REQUEST)
            is CaseError.InvalidCaseDescription -> Problem.InvalidCaseDescription.response(HttpStatus.BAD_REQUEST)
        }

    private fun TokenError.toProblemResponse(): ResponseEntity<Any> =
        when (this) {
            is TokenError.InvalidToken -> Problem.InvalidToken.response(HttpStatus.UNAUTHORIZED)
            is TokenError.ExpiredToken -> Problem.ExpiredToken.response(HttpStatus.UNAUTHORIZED)
        }

    private sealed class CaseAccessResult {
        data class Authorized(
            val case: AccidentCase,
        ) : CaseAccessResult()

        data class Rejected(
            val response: ResponseEntity<Any>,
        ) : CaseAccessResult()
    }

    private sealed class UserAccessResult {
        data class Authenticated(
            val user: User,
        ) : UserAccessResult()

        data class Rejected(
            val response: ResponseEntity<Any>,
        ) : UserAccessResult()
    }
}
