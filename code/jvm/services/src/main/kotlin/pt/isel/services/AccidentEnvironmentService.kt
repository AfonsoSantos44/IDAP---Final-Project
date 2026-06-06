package pt.isel.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.domain.AccidentScene
import pt.isel.domain.WeatherConditions
import pt.isel.repository.TransactionManager

@Service
class AccidentEnvironmentService(
    private val transactionManager: TransactionManager,
    private val weatherApiProvider: WeatherApiProvider,
    private val googleSceneProvider: GoogleSceneProvider,
) {
    private val logger = LoggerFactory.getLogger(AccidentEnvironmentService::class.java)

    fun refreshWeather(
        caseId: Int,
        latitude: Double?,
        longitude: Double?,
    ): Either<AccidentDataError, WeatherConditions> {
        if (latitude != null && !isValidLatitude(latitude)) return failure(AccidentDataError.InvalidAccidentData)
        if (longitude != null && !isValidLongitude(longitude)) return failure(AccidentDataError.InvalidAccidentData)

        val coordinates =
            when (
                val result =
                    transactionManager.run {
                        repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
                        if (latitude != null && longitude != null) {
                            return@run success(GeoCoordinates(latitude, longitude))
                        }

                        val scene =
                            repoAccidentEnvironment.findSceneByCaseId(caseId)
                                ?: return@run failure(AccidentDataError.SceneNotFound)
                        success(GeoCoordinates(scene.latitude, scene.longitude))
                    }
            ) {
                is Success -> result.value
                is Failure -> return result
            }

        val weather =
            when (val result = weatherApiProvider.fetchCurrentWeather(coordinates.latitude, coordinates.longitude)) {
                is Success -> result.value
                is Failure -> {
                    logger.warn(
                        "Weather provider failed for case {} at {}, {}: {}",
                        caseId,
                        coordinates.latitude,
                        coordinates.longitude,
                        result.value.toLogMessage(),
                    )
                    return failure(AccidentDataError.ExternalDataUnavailable)
                }
            }

        return transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)

            val currentWeather = repoAccidentEnvironment.findWeatherByCaseId(caseId)

            success(
                if (currentWeather == null) {
                    repoAccidentEnvironment.insertWeather(
                        caseId = caseId,
                        conditionType = normalizeOptionalText(weather.conditionType),
                        temperature = weather.temperature,
                        visibility = weather.visibility,
                        precipitation = normalizeOptionalText(weather.precipitation),
                    )
                } else {
                    repoAccidentEnvironment.updateWeather(
                        weatherId = currentWeather.weatherId,
                        conditionType = normalizeOptionalText(weather.conditionType),
                        temperature = weather.temperature,
                        visibility = weather.visibility,
                        precipitation = normalizeOptionalText(weather.precipitation),
                    ) ?: currentWeather
                },
            )
        }
    }

    fun getWeatherByCaseId(caseId: Int): Either<AccidentDataError, WeatherConditions> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            repoAccidentEnvironment.findWeatherByCaseId(caseId)?.let { success(it) }
                ?: failure(AccidentDataError.WeatherNotFound)
        }

    fun deleteWeather(caseId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            if (repoAccidentEnvironment.deleteWeatherByCaseId(caseId) == 0) {
                return@run failure(AccidentDataError.WeatherNotFound)
            }
            success(Unit)
        }

    fun refreshScene(
        caseId: Int,
        latitude: Double,
        longitude: Double,
    ): Either<AccidentDataError, AccidentScene> {
        if (!isValidLatitude(latitude)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidLongitude(longitude)) return failure(AccidentDataError.InvalidAccidentData)

        val scene =
            when (val result = googleSceneProvider.fetchScene(latitude, longitude)) {
                is Success -> result.value
                is Failure -> {
                    logger.warn(
                        "Scene provider failed for case {} at {}, {}: {}",
                        caseId,
                        latitude,
                        longitude,
                        result.value.toLogMessage(),
                    )
                    return failure(AccidentDataError.ExternalDataUnavailable)
                }
            }

        return transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)

            val currentScene = repoAccidentEnvironment.findSceneByCaseId(caseId)
            success(
                if (currentScene == null) {
                    repoAccidentEnvironment.insertScene(
                        caseId = caseId,
                        latitude = scene.latitude,
                        longitude = scene.longitude,
                        terrainInclination = scene.terrainInclination,
                        roadGradient = scene.roadGradient,
                        roadType = normalizeRequiredText(scene.roadType, MAX_SHORT_TEXT) ?: "unknown road",
                        spatialDescription =
                            normalizeRequiredText(scene.spatialDescription, MAX_LONG_TEXT)
                                ?: "Scene generated from external provider.",
                        vehiclePositioningNotes =
                            normalizeRequiredText(scene.vehiclePositioningNotes, MAX_LONG_TEXT)
                                ?: "Verify vehicle positioning from uploaded evidence.",
                    )
                } else {
                    repoAccidentEnvironment.updateScene(
                        sceneId = currentScene.sceneId,
                        latitude = scene.latitude,
                        longitude = scene.longitude,
                        terrainInclination = scene.terrainInclination,
                        roadGradient = scene.roadGradient,
                        roadType = normalizeRequiredText(scene.roadType, MAX_SHORT_TEXT) ?: "unknown road",
                        spatialDescription =
                            normalizeRequiredText(scene.spatialDescription, MAX_LONG_TEXT)
                                ?: "Scene generated from external provider.",
                        vehiclePositioningNotes =
                            normalizeRequiredText(scene.vehiclePositioningNotes, MAX_LONG_TEXT)
                                ?: "Verify vehicle positioning from uploaded evidence.",
                    ) ?: currentScene
                },
            )
        }
    }

    fun getSceneByCaseId(caseId: Int): Either<AccidentDataError, AccidentScene> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            repoAccidentEnvironment.findSceneByCaseId(caseId)?.let { success(it) }
                ?: failure(AccidentDataError.SceneNotFound)
        }

    fun deleteScene(caseId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            if (repoAccidentEnvironment.deleteSceneByCaseId(caseId) == 0) {
                return@run failure(AccidentDataError.SceneNotFound)
            }
            success(Unit)
        }
}

private fun ExternalProviderError.toLogMessage(): String =
    when (this) {
        is ExternalProviderError.MissingApiKey -> "missing API key"
        is ExternalProviderError.RequestFailed -> detail
    }
