package pt.isel.repository

import pt.isel.domain.AccidentScene
import pt.isel.domain.WeatherConditions

interface RepositoryAccidentEnvironment {
    fun insertWeather(
        caseId: Int,
        conditionType: String?,
        temperature: Double?,
        visibility: Double?,
        precipitation: String?,
    ): WeatherConditions

    fun findWeatherByCaseId(caseId: Int): WeatherConditions?

    fun updateWeather(
        weatherId: Int,
        conditionType: String?,
        temperature: Double?,
        visibility: Double?,
        precipitation: String?,
    ): WeatherConditions?

    fun deleteWeatherByCaseId(caseId: Int): Int

    fun insertScene(
        caseId: Int,
        latitude: Double,
        longitude: Double,
        terrainInclination: Double,
        roadGradient: Double,
        roadType: String,
        spatialDescription: String,
        vehiclePositioningNotes: String,
    ): AccidentScene

    fun findSceneByCaseId(caseId: Int): AccidentScene?

    fun updateScene(
        sceneId: Int,
        latitude: Double,
        longitude: Double,
        terrainInclination: Double,
        roadGradient: Double,
        roadType: String,
        spatialDescription: String,
        vehiclePositioningNotes: String,
    ): AccidentScene?

    fun deleteSceneByCaseId(caseId: Int): Int
}
