package pt.isel.domain

data class Vehicle(
    val vehicleId: Int,
    val caseId: Int,
    val brand: String,
    val model: String,
    val yearOfFabrication: Int,
    val licensePlate: String,
    val role: String?
)