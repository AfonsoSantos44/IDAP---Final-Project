package pt.isel.services

import com.fasterxml.jackson.databind.JsonNode
import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pt.isel.domain.AccidentSceneSnapshot
import pt.isel.domain.WeatherSnapshot
import java.net.URI
import java.net.URLEncoder
import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets
import java.time.Duration
import kotlin.math.atan
import kotlin.math.pow

sealed class ExternalProviderError {
    data object MissingApiKey : ExternalProviderError()

    data class RequestFailed(
        val detail: String,
    ) : ExternalProviderError()
}

@Service
class WeatherApiProvider(
    private val objectMapper: ObjectMapper,
    @Value("\${idap.weather.api-key:}")
    private val apiKey: String,
    @Value("\${idap.weather.base-url:https://api.weatherapi.com/v1}")
    private val baseUrl: String,
) {
    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    fun fetchCurrentWeather(
        latitude: Double,
        longitude: Double,
    ): Either<ExternalProviderError, WeatherSnapshot> {
        if (apiKey.isBlank()) return failure(ExternalProviderError.MissingApiKey)

        val uri =
            URI.create(
                "${baseUrl.trimEnd('/')}/current.json?key=${encode(apiKey)}&q=$latitude,$longitude&aqi=no",
            )

        return try {
            val response = get(uri)
            if (response.statusCode() !in 200..299) {
                return failure(
                    ExternalProviderError.RequestFailed(
                        providerFailureMessage("WeatherAPI", response.statusCode(), response.body()),
                    ),
                )
            }

            val root = objectMapper.readTree(response.body())
            val current = root.path("current")
            success(
                WeatherSnapshot(
                    conditionType = current.path("condition").path("text").textOrNull(),
                    temperature = current.path("temp_c").doubleOrNull(),
                    visibility = current.path("vis_km").doubleOrNull(),
                    precipitation = current.path("precip_mm").doubleOrNull()?.let { "$it mm" },
                ),
            )
        } catch (ex: Exception) {
            failure(ExternalProviderError.RequestFailed(ex.message ?: "Invalid WeatherAPI response"))
        }
    }

    private fun get(uri: URI): HttpResponse<String> =
        httpClient.send(
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )
}

@Service
class GoogleSceneProvider(
    private val objectMapper: ObjectMapper,
    @Value("\${idap.scene.google-api-key:}")
    private val apiKey: String,
    @Value("\${idap.scene.street-view-metadata-url:https://maps.googleapis.com/maps/api/streetview/metadata}")
    private val streetViewMetadataUrl: String,
    @Value("\${idap.scene.geocoding-url:https://maps.googleapis.com/maps/api/geocode/json}")
    private val geocodingUrl: String,
    @Value("\${idap.scene.elevation-url:https://maps.googleapis.com/maps/api/elevation/json}")
    private val elevationUrl: String,
) {
    private val httpClient: HttpClient =
        HttpClient
            .newBuilder()
            .connectTimeout(Duration.ofSeconds(10))
            .build()

    fun fetchScene(
        latitude: Double,
        longitude: Double,
    ): Either<ExternalProviderError, AccidentSceneSnapshot> {
        if (apiKey.isBlank()) return failure(ExternalProviderError.MissingApiKey)

        return try {
            val streetView = fetchStreetViewMetadata(latitude, longitude)
            val geocode = fetchReverseGeocode(latitude, longitude)
            val gradient = fetchGradient(latitude, longitude)

            success(
                AccidentSceneSnapshot(
                    latitude = streetView?.locationLatitude ?: latitude,
                    longitude = streetView?.locationLongitude ?: longitude,
                    terrainInclination = gradient.terrainInclination,
                    roadGradient = gradient.roadGradient,
                    roadType = geocode.roadType,
                    spatialDescription = buildSpatialDescription(geocode, streetView),
                    vehiclePositioningNotes = buildVehiclePositioningNotes(streetView),
                ),
            )
        } catch (ex: Exception) {
            failure(ExternalProviderError.RequestFailed(ex.message ?: "Scene provider request failed"))
        }
    }

    private fun fetchStreetViewMetadata(
        latitude: Double,
        longitude: Double,
    ): StreetViewMetadata? {
        val uri =
            URI.create(
                "$streetViewMetadataUrl?location=$latitude,$longitude&key=${encode(apiKey)}",
            )
        val response = get(uri)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(providerFailureMessage("Street View", response.statusCode(), response.body()))
        }

        val root = objectMapper.readTree(response.body())
        when (val status = root.path("status").asText()) {
            "OK" -> Unit
            "ZERO_RESULTS", "NOT_FOUND" -> return null
            else -> throw IllegalStateException(providerStatusMessage("Street View", status, root))
        }

        return StreetViewMetadata(
            panoId = root.path("pano_id").textOrNull(),
            date = root.path("date").textOrNull(),
            copyright = root.path("copyright").textOrNull(),
            locationLatitude = root.path("location").path("lat").doubleOrNull(),
            locationLongitude = root.path("location").path("lng").doubleOrNull(),
        )
    }

    private fun fetchReverseGeocode(
        latitude: Double,
        longitude: Double,
    ): ReverseGeocodeData {
        val uri =
            URI.create(
                "$geocodingUrl?latlng=$latitude,$longitude&key=${encode(apiKey)}",
            )
        val response = get(uri)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(providerFailureMessage("Geocoding", response.statusCode(), response.body()))
        }

        val root = objectMapper.readTree(response.body())
        when (val status = root.path("status").asText()) {
            "OK" -> Unit
            "ZERO_RESULTS" ->
                return ReverseGeocodeData(
                    formattedAddress = null,
                    roadType = "unknown road",
                )

            else -> throw IllegalStateException(providerStatusMessage("Geocoding", status, root))
        }
        val firstResult = root.path("results").firstOrNull()

        return ReverseGeocodeData(
            formattedAddress = firstResult?.path("formatted_address")?.textOrNull(),
            roadType = firstResult?.let(::extractRoadType) ?: "unknown road",
        )
    }

    private fun fetchGradient(
        latitude: Double,
        longitude: Double,
    ): GradientData {
        val sampleDistanceMeters = 20.0
        val latitudeOffset = sampleDistanceMeters / METERS_PER_LATITUDE_DEGREE
        val sampleLatitude = latitude + latitudeOffset

        val uri =
            URI.create(
                "$elevationUrl?locations=$latitude,$longitude%7C$sampleLatitude,$longitude&key=${encode(apiKey)}",
            )
        val response = get(uri)
        if (response.statusCode() !in 200..299) {
            throw IllegalStateException(providerFailureMessage("Elevation", response.statusCode(), response.body()))
        }

        val root = objectMapper.readTree(response.body())
        when (val status = root.path("status").asText()) {
            "OK" -> Unit
            "ZERO_RESULTS" -> return GradientData(0.0, 0.0)
            else -> throw IllegalStateException(providerStatusMessage("Elevation", status, root))
        }

        val results = root.path("results")
        if (results.size() < 2) return GradientData(0.0, 0.0)

        val firstElevation = results[0].path("elevation").doubleOrNull() ?: return GradientData(0.0, 0.0)
        val secondElevation = results[1].path("elevation").doubleOrNull() ?: return GradientData(0.0, 0.0)
        val elevationDelta = secondElevation - firstElevation
        val gradientPercent = (elevationDelta / sampleDistanceMeters) * 100.0
        val inclinationDegrees = Math.toDegrees(atan(elevationDelta / sampleDistanceMeters))

        return GradientData(
            terrainInclination = roundTwoDecimals(inclinationDegrees),
            roadGradient = roundTwoDecimals(gradientPercent),
        )
    }

    private fun extractRoadType(result: JsonNode): String {
        val route =
            result
                .path("address_components")
                .firstOrNull { component ->
                    component.path("types").any { it.asText() == "route" }
                }
                ?.path("long_name")
                ?.textOrNull()

        return route ?: result.path("types").firstOrNull()?.asText() ?: "unknown road"
    }

    private fun buildSpatialDescription(
        geocode: ReverseGeocodeData,
        streetView: StreetViewMetadata?,
    ): String =
        listOfNotNull(
            geocode.formattedAddress?.let { "Address: $it" },
            streetView?.panoId?.let { "Street View panorama: $it" },
            streetView?.date?.let { "Street View date: $it" },
            streetView?.copyright,
        ).ifEmpty {
            listOf("Scene generated from Google Maps location metadata.")
        }.joinToString(separator = "\n")

    private fun buildVehiclePositioningNotes(streetView: StreetViewMetadata?): String =
        if (streetView == null) {
            "No Street View panorama was found near the provided coordinates; verify vehicle positioning from uploaded evidence."
        } else {
            "Scene position is based on the nearest Street View panorama metadata; verify vehicle positioning from uploaded evidence."
        }

    private fun get(uri: URI): HttpResponse<String> =
        httpClient.send(
            HttpRequest
                .newBuilder(uri)
                .timeout(Duration.ofSeconds(15))
                .GET()
                .build(),
            HttpResponse.BodyHandlers.ofString(),
        )

    private data class StreetViewMetadata(
        val panoId: String?,
        val date: String?,
        val copyright: String?,
        val locationLatitude: Double?,
        val locationLongitude: Double?,
    )

    private data class ReverseGeocodeData(
        val formattedAddress: String?,
        val roadType: String,
    )

    private data class GradientData(
        val terrainInclination: Double,
        val roadGradient: Double,
    )

    private companion object {
        const val METERS_PER_LATITUDE_DEGREE = 111_320.0
    }
}

private fun JsonNode.textOrNull(): String? = takeIf { !isMissingNode && !isNull }?.asText()

private fun JsonNode.doubleOrNull(): Double? = takeIf { !isMissingNode && !isNull }?.asDouble()?.takeIf { !it.isNaN() && !it.isInfinite() }

private fun JsonNode.firstOrNull(): JsonNode? = if (isArray && size() > 0) get(0) else null

private inline fun JsonNode.firstOrNull(predicate: (JsonNode) -> Boolean): JsonNode? {
    if (!isArray) return null

    for (item in this) {
        if (predicate(item)) return item
    }

    return null
}

private fun encode(value: String): String = URLEncoder.encode(value, StandardCharsets.UTF_8)

private fun roundTwoDecimals(value: Double): Double = kotlin.math.round(value * 10.0.pow(2)) / 10.0.pow(2)

private fun providerFailureMessage(
    provider: String,
    statusCode: Int,
    body: String,
): String {
    val detail = providerErrorFromBody(body)
    return if (detail == null) {
        "$provider returned HTTP $statusCode"
    } else {
        "$provider returned HTTP $statusCode: $detail"
    }
}

private fun providerStatusMessage(
    provider: String,
    status: String,
    root: JsonNode,
): String {
    val detail = root.path("error_message").textOrNull()
    return if (detail == null) {
        "$provider returned status $status"
    } else {
        "$provider returned status $status: $detail"
    }
}

private fun providerErrorFromBody(body: String): String? =
    try {
        val root = ObjectMapper().readTree(body)
        root.path("error").path("message").textOrNull()
            ?: root.path("error_message").textOrNull()
            ?: root.path("status").textOrNull()
    } catch (_: Exception) {
        body.take(200).ifBlank { null }
    }
