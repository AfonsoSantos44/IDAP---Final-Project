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
import pt.isel.http.dto.CaseOutputDto
import pt.isel.http.dto.CreateCaseRequestDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.UpdateCaseRequestDto
import pt.isel.services.CaseError
import pt.isel.services.CaseService
import pt.isel.services.Failure
import pt.isel.services.Success
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
    fun getCases(): ResponseEntity<List<CaseOutputDto>> = ResponseEntity.ok(caseService.getCases().map { it.toOutputDto() })

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
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        when (val result = caseService.getCaseById(id)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }

    @GetMapping(Uris.Cases.LIST_BY_USER_ID)
    fun getCasesByUserId(
        @PathVariable userId: Int,
    ): ResponseEntity<*> =
        when (val result = caseService.getCasesByUserId(userId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }

    @PutMapping(Uris.Cases.UPDATE_BY_ID)
    fun updateCase(
        @PathVariable id: Int,
        @RequestBody request: UpdateCaseRequestDto,
    ): ResponseEntity<*> =
        when (val result = caseService.updateCase(id, request.description, request.status)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }

    @DeleteMapping(Uris.Cases.DELETE_BY_ID)
    fun deleteCaseById(
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        when (val result = caseService.deleteCase(id)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
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
}
