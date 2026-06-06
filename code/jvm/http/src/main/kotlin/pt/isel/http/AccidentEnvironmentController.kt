package pt.isel.http

import org.springframework.http.ResponseEntity
import org.springframework.security.core.annotation.AuthenticationPrincipal
import org.springframework.web.bind.annotation.DeleteMapping
import org.springframework.web.bind.annotation.GetMapping
import org.springframework.web.bind.annotation.PathVariable
import org.springframework.web.bind.annotation.PutMapping
import org.springframework.web.bind.annotation.RequestBody
import org.springframework.web.bind.annotation.RestController
import pt.isel.domain.SecurityPrincipal
import pt.isel.http.dto.RefreshAccidentSceneRequestDto
import pt.isel.http.dto.RefreshWeatherConditionsRequestDto
import pt.isel.services.AccidentEnvironmentService
import pt.isel.services.Failure
import pt.isel.services.Success

@RestController
class AccidentEnvironmentController(
    private val environmentService: AccidentEnvironmentService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Cases.WEATHER)
    fun getWeather(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = environmentService.getWeatherByCaseId(caseId)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Cases.WEATHER)
    fun refreshWeather(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
        @RequestBody request: RefreshWeatherConditionsRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                environmentService.refreshWeather(
                    caseId = caseId,
                    latitude = request.latitude,
                    longitude = request.longitude,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Cases.WEATHER)
    fun deleteWeather(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = environmentService.deleteWeather(caseId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Cases.SCENE)
    fun getScene(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = environmentService.getSceneByCaseId(caseId)) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PutMapping(Uris.Cases.SCENE)
    fun refreshScene(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
        @RequestBody request: RefreshAccidentSceneRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                environmentService.refreshScene(
                    caseId = caseId,
                    latitude = request.latitude,
                    longitude = request.longitude,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Cases.SCENE)
    fun deleteScene(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = environmentService.deleteScene(caseId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
