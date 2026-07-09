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
import pt.isel.domain.SecurityPrincipal
import pt.isel.http.dto.CreateDamageRequestDto
import pt.isel.http.dto.CreateVehicleRequestDto
import pt.isel.http.dto.UpdateDamageRequestDto
import pt.isel.http.dto.UpdateVehicleRequestDto
import pt.isel.services.AccidentVehicleDamageService
import pt.isel.services.Failure
import pt.isel.services.Success

@RestController
class AccidentVehicleDamageController(
    private val vehicleDamageService: AccidentVehicleDamageService,
    private val accessControl: AccidentDataAccessControl,
) {
    @GetMapping(Uris.Cases.VEHICLES)
    fun getVehicles(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (val result = vehicleDamageService.getVehiclesByCaseId(caseId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Cases.VEHICLES)
    fun createVehicle(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable caseId: Int,
        @RequestBody request: CreateVehicleRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeCase(currentUser, caseId)) {
            is CaseAccessResult.Authorized -> Unit
            is CaseAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                vehicleDamageService.createVehicle(
                    caseId = caseId,
                    brand = request.brand,
                    model = request.model,
                    yearOfFabrication = request.yearOfFabrication,
                    licensePlate = request.licensePlate,
                    color = request.color,
                    role = request.role,
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/vehicles/${result.value.vehicleId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Vehicles.GET_BY_ID)
    fun getVehicle(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable vehicleId: Int,
    ): ResponseEntity<*> =
        when (val access = accessControl.authorizeVehicle(currentUser, vehicleId)) {
            is VehicleAccessResult.Authorized -> ResponseEntity.ok(access.vehicle.toOutputDto())
            is VehicleAccessResult.Rejected -> access.response
        }

    @PutMapping(Uris.Vehicles.UPDATE_BY_ID)
    fun updateVehicle(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable vehicleId: Int,
        @RequestBody request: UpdateVehicleRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeVehicle(currentUser, vehicleId)) {
            is VehicleAccessResult.Authorized -> Unit
            is VehicleAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                vehicleDamageService.updateVehicle(
                    vehicleId = vehicleId,
                    brand = request.brand,
                    model = request.model,
                    yearOfFabrication = request.yearOfFabrication,
                    licensePlate = request.licensePlate,
                    color = request.color,
                    role = request.role,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Vehicles.DELETE_BY_ID)
    fun deleteVehicle(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable vehicleId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeVehicle(currentUser, vehicleId)) {
            is VehicleAccessResult.Authorized -> Unit
            is VehicleAccessResult.Rejected -> return access.response
        }

        return when (val result = vehicleDamageService.deleteVehicle(vehicleId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Vehicles.DAMAGES)
    fun getDamages(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable vehicleId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeVehicle(currentUser, vehicleId)) {
            is VehicleAccessResult.Authorized -> Unit
            is VehicleAccessResult.Rejected -> return access.response
        }

        return when (val result = vehicleDamageService.getDamagesByVehicleId(vehicleId)) {
            is Success -> ResponseEntity.ok(result.value.map { it.toOutputDto() })
            is Failure -> result.value.toProblemResponse()
        }
    }

    @PostMapping(Uris.Vehicles.DAMAGES)
    fun createDamage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable vehicleId: Int,
        @RequestBody request: CreateDamageRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeVehicle(currentUser, vehicleId)) {
            is VehicleAccessResult.Authorized -> Unit
            is VehicleAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                vehicleDamageService.createDamage(
                    vehicleId = vehicleId,
                    contactZone = request.contactZone,
                    deformationType = request.deformationType,
                    direction = request.direction,
                    damageDescription = request.damageDescription,
                )
        ) {
            is Success ->
                ResponseEntity
                    .status(HttpStatus.CREATED)
                    .header("Location", "/api/damages/${result.value.damageId}")
                    .body(result.value.toOutputDto())

            is Failure -> result.value.toProblemResponse()
        }
    }

    @GetMapping(Uris.Damages.GET_BY_ID)
    fun getDamage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable damageId: Int,
    ): ResponseEntity<*> =
        when (val access = accessControl.authorizeDamage(currentUser, damageId)) {
            is DamageAccessResult.Authorized -> ResponseEntity.ok(access.damage.toOutputDto())
            is DamageAccessResult.Rejected -> access.response
        }

    @PutMapping(Uris.Damages.UPDATE_BY_ID)
    fun updateDamage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable damageId: Int,
        @RequestBody request: UpdateDamageRequestDto,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeDamage(currentUser, damageId)) {
            is DamageAccessResult.Authorized -> Unit
            is DamageAccessResult.Rejected -> return access.response
        }

        return when (
            val result =
                vehicleDamageService.updateDamage(
                    damageId = damageId,
                    contactZone = request.contactZone,
                    deformationType = request.deformationType,
                    direction = request.direction,
                    damageDescription = request.damageDescription,
                )
        ) {
            is Success -> ResponseEntity.ok(result.value.toOutputDto())
            is Failure -> result.value.toProblemResponse()
        }
    }

    @DeleteMapping(Uris.Damages.DELETE_BY_ID)
    fun deleteDamage(
        @AuthenticationPrincipal currentUser: SecurityPrincipal?,
        @PathVariable damageId: Int,
    ): ResponseEntity<*> {
        when (val access = accessControl.authorizeDamage(currentUser, damageId)) {
            is DamageAccessResult.Authorized -> Unit
            is DamageAccessResult.Rejected -> return access.response
        }

        return when (val result = vehicleDamageService.deleteDamage(damageId)) {
            is Success -> ResponseEntity.noContent().build<Unit>()
            is Failure -> result.value.toProblemResponse()
        }
    }
}
