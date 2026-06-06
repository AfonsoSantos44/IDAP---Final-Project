package pt.isel.repository

import pt.isel.domain.Damage
import pt.isel.domain.Vehicle

interface RepositoryAccidentVehicleDamage {
    fun createVehicle(
        caseId: Int,
        brand: String,
        model: String,
        yearOfFabrication: Int,
        licensePlate: String,
        role: String?,
    ): Vehicle

    fun findVehicleById(vehicleId: Int): Vehicle?

    fun findVehicleByCaseIdAndLicensePlate(
        caseId: Int,
        licensePlate: String,
    ): Vehicle?

    fun findVehiclesByCaseId(caseId: Int): List<Vehicle>

    fun updateVehicle(
        vehicleId: Int,
        brand: String,
        model: String,
        yearOfFabrication: Int,
        licensePlate: String,
        role: String?,
    ): Vehicle?

    fun deleteVehicleById(vehicleId: Int): Int

    fun createDamage(
        vehicleId: Int,
        contactZone: String,
        deformationType: String,
        direction: String,
        damageDescription: String,
    ): Damage

    fun findDamageById(damageId: Int): Damage?

    fun findDamagesByVehicleId(vehicleId: Int): List<Damage>

    fun updateDamage(
        damageId: Int,
        contactZone: String,
        deformationType: String,
        direction: String,
        damageDescription: String,
    ): Damage?

    fun updateDamageHeight(
        damageId: Int,
        heightCm: Double,
    ): Damage?

    fun deleteDamageById(damageId: Int): Int
}
