package pt.isel.domain

data class Damage(
    val damageId: Int,
    val vehicleId: Int,
    val contactZone: String,
    val deformationType: String,
    val direction: String,
    val heightCm: Double,
    val damageDescription: String,
)
