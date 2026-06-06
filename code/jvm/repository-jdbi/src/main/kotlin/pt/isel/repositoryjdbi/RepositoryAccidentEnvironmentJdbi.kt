package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.AccidentScene
import pt.isel.domain.WeatherConditions
import pt.isel.repository.RepositoryAccidentEnvironment
import pt.isel.repositoryjdbi.mappers.AccidentSceneMapper
import pt.isel.repositoryjdbi.mappers.WeatherConditionsMapper

class RepositoryAccidentEnvironmentJdbi(
    private val handle: Handle,
) : RepositoryAccidentEnvironment {
    override fun insertWeather(
        caseId: Int,
        conditionType: String?,
        temperature: Double?,
        visibility: Double?,
        precipitation: String?,
    ): WeatherConditions =
        handle.createUpdate(
            """
            INSERT INTO weather_conditions (case_id, condition_type, temperature, visibility, precipitation)
            VALUES (:case_id, :condition_type, :temperature, :visibility, :precipitation)
            """,
        )
            .bind("case_id", caseId)
            .bind("condition_type", conditionType)
            .bind("temperature", temperature)
            .bind("visibility", visibility)
            .bind("precipitation", precipitation)
            .executeAndReturnGeneratedKeys(
                "weather_id",
                "case_id",
                "condition_type",
                "temperature",
                "visibility",
                "precipitation",
            )
            .map(WeatherConditionsMapper())
            .one()

    override fun findWeatherByCaseId(caseId: Int): WeatherConditions? =
        handle.createQuery("SELECT * FROM weather_conditions WHERE case_id = :case_id")
            .bind("case_id", caseId)
            .map(WeatherConditionsMapper())
            .singleOrNull()

    override fun updateWeather(
        weatherId: Int,
        conditionType: String?,
        temperature: Double?,
        visibility: Double?,
        precipitation: String?,
    ): WeatherConditions? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE weather_conditions
                SET condition_type = :condition_type,
                    temperature = :temperature,
                    visibility = :visibility,
                    precipitation = :precipitation
                WHERE weather_id = :weather_id
                """,
            )
                .bind("weather_id", weatherId)
                .bind("condition_type", conditionType)
                .bind("temperature", temperature)
                .bind("visibility", visibility)
                .bind("precipitation", precipitation)
                .execute()

        return if (rowsUpdated == 0) null else findWeatherById(weatherId)
    }

    override fun deleteWeatherByCaseId(caseId: Int): Int =
        handle.createUpdate("DELETE FROM weather_conditions WHERE case_id = :case_id")
            .bind("case_id", caseId)
            .execute()

    override fun insertScene(
        caseId: Int,
        latitude: Double,
        longitude: Double,
        terrainInclination: Double,
        roadGradient: Double,
        roadType: String,
        spatialDescription: String,
        vehiclePositioningNotes: String,
    ): AccidentScene =
        handle.createUpdate(
            """
            INSERT INTO accident_scene (
                case_id,
                latitude,
                longitude,
                terrain_inclination,
                road_gradient,
                road_type,
                spatial_description,
                vehicle_positioning_notes
            )
            VALUES (
                :case_id,
                :latitude,
                :longitude,
                :terrain_inclination,
                :road_gradient,
                :road_type,
                :spatial_description,
                :vehicle_positioning_notes
            )
            """,
        )
            .bind("case_id", caseId)
            .bind("latitude", latitude)
            .bind("longitude", longitude)
            .bind("terrain_inclination", terrainInclination)
            .bind("road_gradient", roadGradient)
            .bind("road_type", roadType)
            .bind("spatial_description", spatialDescription)
            .bind("vehicle_positioning_notes", vehiclePositioningNotes)
            .executeAndReturnGeneratedKeys(
                "scene_id",
                "case_id",
                "latitude",
                "longitude",
                "terrain_inclination",
                "road_gradient",
                "road_type",
                "spatial_description",
                "vehicle_positioning_notes",
            )
            .map(AccidentSceneMapper())
            .one()

    override fun findSceneByCaseId(caseId: Int): AccidentScene? =
        handle.createQuery("SELECT * FROM accident_scene WHERE case_id = :case_id")
            .bind("case_id", caseId)
            .map(AccidentSceneMapper())
            .singleOrNull()

    override fun updateScene(
        sceneId: Int,
        latitude: Double,
        longitude: Double,
        terrainInclination: Double,
        roadGradient: Double,
        roadType: String,
        spatialDescription: String,
        vehiclePositioningNotes: String,
    ): AccidentScene? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE accident_scene
                SET latitude = :latitude,
                    longitude = :longitude,
                    terrain_inclination = :terrain_inclination,
                    road_gradient = :road_gradient,
                    road_type = :road_type,
                    spatial_description = :spatial_description,
                    vehicle_positioning_notes = :vehicle_positioning_notes
                WHERE scene_id = :scene_id
                """,
            )
                .bind("scene_id", sceneId)
                .bind("latitude", latitude)
                .bind("longitude", longitude)
                .bind("terrain_inclination", terrainInclination)
                .bind("road_gradient", roadGradient)
                .bind("road_type", roadType)
                .bind("spatial_description", spatialDescription)
                .bind("vehicle_positioning_notes", vehiclePositioningNotes)
                .execute()

        return if (rowsUpdated == 0) null else findSceneById(sceneId)
    }

    override fun deleteSceneByCaseId(caseId: Int): Int =
        handle.createUpdate("DELETE FROM accident_scene WHERE case_id = :case_id")
            .bind("case_id", caseId)
            .execute()

    private fun findWeatherById(weatherId: Int): WeatherConditions? =
        handle.createQuery("SELECT * FROM weather_conditions WHERE weather_id = :weather_id")
            .bind("weather_id", weatherId)
            .map(WeatherConditionsMapper())
            .singleOrNull()

    private fun findSceneById(sceneId: Int): AccidentScene? =
        handle.createQuery("SELECT * FROM accident_scene WHERE scene_id = :scene_id")
            .bind("scene_id", sceneId)
            .map(AccidentSceneMapper())
            .singleOrNull()
}
