package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.Damage
import pt.isel.domain.Vehicle
import pt.isel.repository.RepositoryAccidentVehicleDamage
import pt.isel.repositoryjdbi.mappers.DamageMapper
import pt.isel.repositoryjdbi.mappers.VehicleMapper

class RepositoryAccidentVehicleDamageJdbi(
    private val handle: Handle,
) : RepositoryAccidentVehicleDamage {
    override fun createVehicle(
        caseId: Int,
        brand: String,
        model: String,
        yearOfFabrication: Int,
        licensePlate: String,
        color: String?,
        role: String?,
    ): Vehicle =
        handle.createUpdate(
            """
            INSERT INTO vehicle (case_id, brand, model, year_of_fabrication, license_plate, color, role)
            VALUES (:case_id, :brand, :model, :year_of_fabrication, :license_plate, :color, :role)
            """,
        )
            .bind("case_id", caseId)
            .bind("brand", brand)
            .bind("model", model)
            .bind("year_of_fabrication", yearOfFabrication)
            .bind("license_plate", licensePlate)
            .bind("color", color)
            .bind("role", role)
            .executeAndReturnGeneratedKeys(
                "vehicle_id",
                "case_id",
                "brand",
                "model",
                "year_of_fabrication",
                "license_plate",
                "color",
                "role",
            )
            .map(VehicleMapper())
            .one()

    override fun findVehicleById(vehicleId: Int): Vehicle? =
        handle.createQuery("SELECT * FROM vehicle WHERE vehicle_id = :vehicle_id")
            .bind("vehicle_id", vehicleId)
            .map(VehicleMapper())
            .singleOrNull()

    override fun findVehicleByCaseIdAndLicensePlate(
        caseId: Int,
        licensePlate: String,
    ): Vehicle? =
        handle.createQuery(
            """
            SELECT *
            FROM vehicle
            WHERE case_id = :case_id AND lower(license_plate) = lower(:license_plate)
            """,
        )
            .bind("case_id", caseId)
            .bind("license_plate", licensePlate)
            .map(VehicleMapper())
            .singleOrNull()

    override fun findVehiclesByCaseId(caseId: Int): List<Vehicle> =
        handle.createQuery("SELECT * FROM vehicle WHERE case_id = :case_id ORDER BY vehicle_id ASC")
            .bind("case_id", caseId)
            .map(VehicleMapper())
            .list()

    override fun updateVehicle(
        vehicleId: Int,
        brand: String,
        model: String,
        yearOfFabrication: Int,
        licensePlate: String,
        color: String?,
        role: String?,
    ): Vehicle? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE vehicle
                SET brand = :brand,
                    model = :model,
                    year_of_fabrication = :year_of_fabrication,
                    license_plate = :license_plate,
                    color = :color,
                    role = :role
                WHERE vehicle_id = :vehicle_id
                """,
            )
                .bind("vehicle_id", vehicleId)
                .bind("brand", brand)
                .bind("model", model)
                .bind("year_of_fabrication", yearOfFabrication)
                .bind("license_plate", licensePlate)
                .bind("color", color)
                .bind("role", role)
                .execute()

        return if (rowsUpdated == 0) null else findVehicleById(vehicleId)
    }

    override fun deleteVehicleById(vehicleId: Int): Int =
        handle.createUpdate("DELETE FROM vehicle WHERE vehicle_id = :vehicle_id")
            .bind("vehicle_id", vehicleId)
            .execute()

    override fun createDamage(
        vehicleId: Int,
        contactZone: String,
        deformationType: String,
        direction: String,
        damageDescription: String,
    ): Damage =
        handle.createUpdate(
            """
            INSERT INTO damage (
                vehicle_id,
                contact_zone,
                deformation_type,
                direction,
                damage_description
            )
            VALUES (
                :vehicle_id,
                :contact_zone,
                :deformation_type,
                :direction,
                :damage_description
            )
            """,
        )
            .bind("vehicle_id", vehicleId)
            .bind("contact_zone", contactZone)
            .bind("deformation_type", deformationType)
            .bind("direction", direction)
            .bind("damage_description", damageDescription)
            .executeAndReturnGeneratedKeys(
                "damage_id",
                "vehicle_id",
                "contact_zone",
                "deformation_type",
                "direction",
                "height_cm",
                "damage_description",
            )
            .map(DamageMapper())
            .one()

    override fun findDamageById(damageId: Int): Damage? =
        handle.createQuery("SELECT * FROM damage WHERE damage_id = :damage_id")
            .bind("damage_id", damageId)
            .map(DamageMapper())
            .singleOrNull()

    override fun findDamagesByVehicleId(vehicleId: Int): List<Damage> =
        handle.createQuery("SELECT * FROM damage WHERE vehicle_id = :vehicle_id ORDER BY damage_id ASC")
            .bind("vehicle_id", vehicleId)
            .map(DamageMapper())
            .list()

    override fun updateDamage(
        damageId: Int,
        contactZone: String,
        deformationType: String,
        direction: String,
        damageDescription: String,
    ): Damage? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE damage
                SET contact_zone = :contact_zone,
                    deformation_type = :deformation_type,
                    direction = :direction,
                    damage_description = :damage_description
                WHERE damage_id = :damage_id
                """,
            )
                .bind("damage_id", damageId)
                .bind("contact_zone", contactZone)
                .bind("deformation_type", deformationType)
                .bind("direction", direction)
                .bind("damage_description", damageDescription)
                .execute()

        return if (rowsUpdated == 0) null else findDamageById(damageId)
    }

    override fun updateDamageHeight(
        damageId: Int,
        heightCm: Double,
    ): Damage? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE damage
                SET height_cm = :height_cm
                WHERE damage_id = :damage_id
                """,
            )
                .bind("damage_id", damageId)
                .bind("height_cm", heightCm)
                .execute()

        return if (rowsUpdated == 0) null else findDamageById(damageId)
    }

    override fun deleteDamageById(damageId: Int): Int =
        handle.createUpdate("DELETE FROM damage WHERE damage_id = :damage_id")
            .bind("damage_id", damageId)
            .execute()
}
