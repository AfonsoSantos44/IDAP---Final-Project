package pt.isel.domain

data class WeatherConditions(
    val weatherId: Int,
    val caseId: Int,
    val conditionType: String?,
    val temperature: Double?,
    val visibility: Double?,
    val precipitation: String?
)