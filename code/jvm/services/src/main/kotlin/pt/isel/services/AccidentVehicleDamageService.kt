package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.Damage
import pt.isel.domain.Vehicle
import pt.isel.repository.TransactionManager

@Service
class AccidentVehicleDamageService(
    private val transactionManager: TransactionManager,
) {
    fun createVehicle(
        caseId: Int,
        brand: String,
        model: String,
        yearOfFabrication: Int,
        licensePlate: String,
        role: String?,
    ): Either<AccidentDataError, Vehicle> {
        val normalizedBrand =
            normalizeRequiredText(brand, MAX_SHORT_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedModel =
            normalizeRequiredText(model, MAX_SHORT_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedLicensePlate =
            normalizeRequiredText(licensePlate, MAX_LICENSE_PLATE)
                ?: return failure(AccidentDataError.InvalidAccidentData)

        if (!isValidVehicleYear(yearOfFabrication)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(role, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            repoAccidentVehicleDamage.findVehicleByCaseIdAndLicensePlate(caseId, normalizedLicensePlate)?.let {
                return@run failure(AccidentDataError.DuplicateVehicle)
            }

            success(
                repoAccidentVehicleDamage.createVehicle(
                    caseId = caseId,
                    brand = normalizedBrand,
                    model = normalizedModel,
                    yearOfFabrication = yearOfFabrication,
                    licensePlate = normalizedLicensePlate,
                    role = normalizeOptionalText(role),
                ),
            )
        }
    }

    fun getVehiclesByCaseId(caseId: Int): Either<AccidentDataError, List<Vehicle>> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            success(repoAccidentVehicleDamage.findVehiclesByCaseId(caseId))
        }

    fun getVehicleById(vehicleId: Int): Either<AccidentDataError, Vehicle> =
        transactionManager.run {
            repoAccidentVehicleDamage.findVehicleById(vehicleId)?.let { success(it) }
                ?: failure(AccidentDataError.VehicleNotFound)
        }

    fun updateVehicle(
        vehicleId: Int,
        brand: String?,
        model: String?,
        yearOfFabrication: Int?,
        licensePlate: String?,
        role: String?,
    ): Either<AccidentDataError, Vehicle> {
        if (!isValidOptionalText(brand, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(model, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(licensePlate, MAX_LICENSE_PLATE)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(role, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (yearOfFabrication != null && !isValidVehicleYear(yearOfFabrication)) {
            return failure(AccidentDataError.InvalidAccidentData)
        }

        return transactionManager.run {
            val currentVehicle =
                repoAccidentVehicleDamage.findVehicleById(vehicleId) ?: return@run failure(AccidentDataError.VehicleNotFound)
            val normalizedLicensePlate = normalizeOptionalText(licensePlate) ?: currentVehicle.licensePlate
            val duplicateVehicle =
                repoAccidentVehicleDamage.findVehicleByCaseIdAndLicensePlate(currentVehicle.caseId, normalizedLicensePlate)

            if (duplicateVehicle != null && duplicateVehicle.vehicleId != vehicleId) {
                return@run failure(AccidentDataError.DuplicateVehicle)
            }

            success(
                repoAccidentVehicleDamage.updateVehicle(
                    vehicleId = vehicleId,
                    brand = normalizeOptionalText(brand) ?: currentVehicle.brand,
                    model = normalizeOptionalText(model) ?: currentVehicle.model,
                    yearOfFabrication = yearOfFabrication ?: currentVehicle.yearOfFabrication,
                    licensePlate = normalizedLicensePlate,
                    role = if (role == null) currentVehicle.role else normalizeOptionalText(role),
                ) ?: currentVehicle,
            )
        }
    }

    fun deleteVehicle(vehicleId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentVehicleDamage.findVehicleById(vehicleId) ?: return@run failure(AccidentDataError.VehicleNotFound)
            repoAccidentVehicleDamage.deleteVehicleById(vehicleId)
            success(Unit)
        }

    fun createDamage(
        vehicleId: Int,
        contactZone: String,
        deformationType: String,
        direction: String,
        damageDescription: String,
    ): Either<AccidentDataError, Damage> {
        val normalizedContactZone =
            normalizeRequiredText(contactZone, MAX_SHORT_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedDeformationType =
            normalizeRequiredText(deformationType, MAX_SHORT_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedDirection =
            normalizeRequiredText(direction, MAX_SHORT_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedDescription =
            normalizeRequiredText(damageDescription, MAX_LONG_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            repoAccidentVehicleDamage.findVehicleById(vehicleId) ?: return@run failure(AccidentDataError.VehicleNotFound)

            success(
                repoAccidentVehicleDamage.createDamage(
                    vehicleId = vehicleId,
                    contactZone = normalizedContactZone,
                    deformationType = normalizedDeformationType,
                    direction = normalizedDirection,
                    damageDescription = normalizedDescription,
                ),
            )
        }
    }

    fun getDamagesByVehicleId(vehicleId: Int): Either<AccidentDataError, List<Damage>> =
        transactionManager.run {
            repoAccidentVehicleDamage.findVehicleById(vehicleId) ?: return@run failure(AccidentDataError.VehicleNotFound)
            success(repoAccidentVehicleDamage.findDamagesByVehicleId(vehicleId))
        }

    fun getDamageById(damageId: Int): Either<AccidentDataError, Damage> =
        transactionManager.run {
            repoAccidentVehicleDamage.findDamageById(damageId)?.let { success(it) }
                ?: failure(AccidentDataError.DamageNotFound)
        }

    fun updateDamage(
        damageId: Int,
        contactZone: String?,
        deformationType: String?,
        direction: String?,
        damageDescription: String?,
    ): Either<AccidentDataError, Damage> {
        if (!isValidOptionalText(contactZone, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(deformationType, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(direction, MAX_SHORT_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(damageDescription, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val currentDamage =
                repoAccidentVehicleDamage.findDamageById(damageId) ?: return@run failure(AccidentDataError.DamageNotFound)

            success(
                repoAccidentVehicleDamage.updateDamage(
                    damageId = damageId,
                    contactZone = normalizeOptionalText(contactZone) ?: currentDamage.contactZone,
                    deformationType = normalizeOptionalText(deformationType) ?: currentDamage.deformationType,
                    direction = normalizeOptionalText(direction) ?: currentDamage.direction,
                    damageDescription = normalizeOptionalText(damageDescription) ?: currentDamage.damageDescription,
                ) ?: currentDamage,
            )
        }
    }

    fun deleteDamage(damageId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentVehicleDamage.findDamageById(damageId) ?: return@run failure(AccidentDataError.DamageNotFound)
            repoAccidentVehicleDamage.deleteDamageById(damageId)
            success(Unit)
        }
}
