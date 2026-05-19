package pt.isel.http

import org.springframework.http.HttpStatus
import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PostMapping
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.AccidentCase
import pt.isel.domain.SecurityPrincipal
import pt.isel.http.dto.CaseOutputDto
import pt.isel.http.dto.CreateCaseRequestDto
import pt.isel.http.dto.Problem
import pt.isel.http.dto.UpdateCaseRequestDto
import pt.isel.services.CaseError
import pt.isel.services.CaseService
import pt.isel.services.Failure
import pt.isel.services.Success
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter

@RestController
class CaseController(
    private val caseService: CaseService,
) {
    companion object {
        private val createdAtFormatter: DateTimeFormatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")
                .withZone(ZoneId.systemDefault())
    }

    @GetMapping(Uris.Cases.LIST)
    fun getCases(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
    ): ResponseEntity<*> {
        val authenticatedUser = currentUser ?: return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)

        val cases =
            if (authenticatedUser.isAdmin()) {
                caseService.getCases()
            } else {
                when (val result = caseService.getCasesByUserId(authenticatedUser.userId)) {
                    is Success -> result.value
                    is Failure -> return result.value.toProblemResponse()
                }
            }

        return ResponseEntity.ok(cases.map { it.toOutputDto() })
    }

    @PostMapping(Uris.Cases.CREATE)
    fun createCase(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @RequestBody request: CreateCaseRequestDto,
    ): ResponseEntity<*> {
        val authenticatedUser = currentUser ?: return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)
        val caseOwnerId =
            if (authenticatedUser.isAdmin()) {
                request.userId ?: authenticatedUser.userId
            } else {
                authenticatedUser.userId
            }

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
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable id: Int,
    ): ResponseEntity<*> =
        when (val result = getAuthorizedCase(currentUser, id)) {
            is CaseAccessResult.Authorized -> ResponseEntity.ok(result.case.toOutputDto())
            is CaseAccessResult.Rejected -> result.response
        }

    @GetMapping(Uris.Cases.LIST_BY_USER_ID)
    fun getCasesByUserId(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable userId: Int,
    ): ResponseEntity<*> {
        val authenticatedUser = currentUser ?: return Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED)

        if (!authenticatedUser.isAdmin() && authenticatedUser.userId != userId) {
            return Problem.CaseAccessDenied.response(HttpStatus.FORBIDDEN)
        }

        return when (val result = caseService.getCasesByUserId(userId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Cases.UPDATE_BY_ID)
    fun updateCase(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable id: Int,
        @RequestBody request: UpdateCaseRequestDto,
    ): ResponseEntity<*> {
        when (val accessResult = getAuthorizedCase(currentUser, id)) {
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
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable id: Int,
    ): ResponseEntity<*> {
        when (val accessResult = getAuthorizedCase(currentUser, id)) {
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

    private fun SecurityPrincipal.canAccess(accidentCase: AccidentCase): Boolean = isAdmin() || userId == accidentCase.userId

    private fun getAuthorizedCase(
        currentUser: SecurityPrincipal?,
        caseId: Int,
    ): CaseAccessResult {
        val authenticatedUser =
            currentUser
                ?: return CaseAccessResult.Rejected(Problem.NoUserLoggedIn.response(HttpStatus.UNAUTHORIZED))

        val accidentCase =
            when (val result = caseService.getCaseById(caseId)) {
                is Success -> result.value
                is Failure -> return CaseAccessResult.Rejected(result.value.toProblemResponse())
            }

        if (!authenticatedUser.canAccess(accidentCase)) {
            return CaseAccessResult.Rejected(Problem.CaseAccessDenied.response(HttpStatus.FORBIDDEN))
        }

        return CaseAccessResult.Authorized(accidentCase)
    }

    private fun CaseError.toProblemResponse(): ResponseEntity<Any> =
        when (this) {
            is CaseError.UserNotFound -> Problem.UserNotFound.response(HttpStatus.NOT_FOUND)
            is CaseError.CaseNotFound -> Problem.CaseNotFound.response(HttpStatus.NOT_FOUND)
            is CaseError.InvalidCaseStatus -> Problem.InvalidCaseStatus.response(HttpStatus.BAD_REQUEST)
            is CaseError.InvalidCaseDescription -> Problem.InvalidCaseDescription.response(HttpStatus.BAD_REQUEST)
        }

    private sealed class CaseAccessResult {
        data class Authorized(
            val case: AccidentCase,
        ) : CaseAccessResult()

        data class Rejected(
            val response: ResponseEntity<Any>,
        ) : CaseAccessResult()
    }
}
