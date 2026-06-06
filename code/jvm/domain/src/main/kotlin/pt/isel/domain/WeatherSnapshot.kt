package pt.isel.domain

data class WeatherSnapshot(
    val conditionType: String?,
    val temperature: Double?,
    val visibility: Double?,
    val precipitation: String?,
)
