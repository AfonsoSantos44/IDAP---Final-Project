package pt.isel.services

import com.fasterxml.jackson.databind.ObjectMapper
import io.minio.GetPresignedObjectUrlArgs
import io.minio.MinioClient
import io.minio.http.Method
import org.slf4j.LoggerFactory
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pt.isel.domain.ComputedMeasurement
import java.nio.charset.StandardCharsets
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID
import java.util.concurrent.TimeUnit
import kotlin.io.path.absolute

private data class MeasurementEngineRequest(
    val primaryImageUrl: String,
    val comparisonImageUrl: String?,
    val outputImageKey: String,
    val outputImageUrl: String,
    val knownTickDistanceCm: Double?,
    val primarySelection: DamageSelectionInput,
    val primaryCalibration: RulerCalibrationInput?,
    val comparisonSelection: DamageSelectionInput?,
    val comparisonCalibration: RulerCalibrationInput?,
)

@Service
class MeasurementEngine(
    private val objectMapper: ObjectMapper,
    @Value("\${idap.measurement.python-executable:python}")
    private val pythonExecutable: String,
    @Value("\${idap.measurement.script-path:Image_analysis/measurement_engine.py}")
    private val scriptPath: String,
    @Value("\${idap.measurement.output-dir:build/idap-measurements}")
    private val outputDirectory: String,
    @Value("\${idap.measurement.timeout-seconds:90}")
    private val timeoutSeconds: Long,
    @Value("\${idap.measurement.tesseract-executable:}")
    private val tesseractExecutable: String,
    @Value("\${idap.storage.endpoint:http://localhost:9000}")
    private val storageEndpoint: String,
    @Value("\${idap.storage.access-key:minioadmin}")
    private val storageAccessKey: String,
    @Value("\${idap.storage.secret-key:minioadmin}")
    private val storageSecretKey: String,
    @Value("\${idap.storage.bucket:idap-images}")
    private val storageBucket: String,
) {
    private val logger = LoggerFactory.getLogger(MeasurementEngine::class.java)

    private val storageClient: MinioClient =
        MinioClient.builder()
            .endpoint(storageEndpoint)
            .credentials(storageAccessKey, storageSecretKey)
            .build()

    fun measure(
        primaryImageKey: String,
        comparisonImageKey: String?,
        knownTickDistanceCm: Double?,
        primarySelection: DamageSelectionInput,
        primaryCalibration: RulerCalibrationInput?,
        comparisonSelection: DamageSelectionInput?,
        comparisonCalibration: RulerCalibrationInput?,
    ): Either<MeasurementEngineError, ComputedMeasurement> {
        val script =
            resolveRegularFile(scriptPath)
                ?: return failure(MeasurementEngineError.ScriptNotFound("Script not found: $scriptPath"))

        val outputImageKey = "measurements/${UUID.randomUUID()}.jpg"

        logger.info(
            "Measurement engine object keys: primary={}, comparison={}, output={}",
            primaryImageKey,
            comparisonImageKey,
            outputImageKey,
        )

        val request =
            MeasurementEngineRequest(
                primaryImageUrl = presignedUrl(Method.GET, primaryImageKey),
                comparisonImageUrl = comparisonImageKey?.let { presignedUrl(Method.GET, it) },
                outputImageKey = outputImageKey,
                outputImageUrl = presignedUrl(Method.PUT, outputImageKey),
                knownTickDistanceCm = knownTickDistanceCm,
                primarySelection = primarySelection,
                primaryCalibration = primaryCalibration,
                comparisonSelection = comparisonSelection,
                comparisonCalibration = comparisonCalibration,
            )

        val processBuilder =
            ProcessBuilder(pythonExecutable, script.toString())
                .redirectErrorStream(false)
        processBuilder.environment()["PYTHONUTF8"] = "1"
        processBuilder.environment()["PYTHONIOENCODING"] = "UTF-8"
        if (tesseractExecutable.isNotBlank()) {
            processBuilder.environment()["TESSERACT_CMD"] = tesseractExecutable
        }

        val process = processBuilder.start()

        process.outputStream.bufferedWriter(StandardCharsets.UTF_8).use { writer ->
            objectMapper.writeValue(writer, request)
        }

        val finished = process.waitFor(timeoutSeconds.coerceAtLeast(1), TimeUnit.SECONDS)
        if (!finished) {
            process.destroyForcibly()
            return failure(MeasurementEngineError.Timeout)
        }

        val stdout = process.inputStream.readAllBytes().toString(StandardCharsets.UTF_8)
        val stderr = process.errorStream.readAllBytes().toString(StandardCharsets.UTF_8)

        if (process.exitValue() != 0) {
            return failure(
                MeasurementEngineError.ProcessingFailed(
                    stderr.ifBlank { stdout.ifBlank { "Measurement engine exited with ${process.exitValue()}" } },
                ),
            )
        }

        return try {
            success(objectMapper.readValue(stdout, ComputedMeasurement::class.java))
        } catch (ex: Exception) {
            failure(MeasurementEngineError.InvalidEngineResponse(ex.message ?: "Invalid measurement engine response"))
        }
    }

    private fun resolveRegularFile(path: String): Path? {
        val candidate = Path.of(path)
        if (candidate.isAbsolute) {
            return candidate.normalize().takeIf { Files.isRegularFile(it) }
        }

        return generateSequence(Path.of("").absolute().normalize()) { it.parent }
            .map { it.resolve(candidate).normalize() }
            .firstOrNull { Files.isRegularFile(it) }
    }

    private fun presignedUrl(
        method: Method,
        key: String,
    ): String =
        storageClient.getPresignedObjectUrl(
            GetPresignedObjectUrlArgs.builder()
                .method(method)
                .bucket(storageBucket)
                .`object`(key)
                .expiry(timeoutSeconds.coerceAtLeast(60).coerceAtMost(604800).toInt())
                .build(),
        )
}

sealed class MeasurementEngineError {
    data class ScriptNotFound(
        val detail: String,
    ) : MeasurementEngineError()

    data class InputImageNotFound(
        val detail: String,
    ) : MeasurementEngineError()

    data object Timeout : MeasurementEngineError()

    data class ProcessingFailed(
        val detail: String,
    ) : MeasurementEngineError()

    data class InvalidEngineResponse(
        val detail: String,
    ) : MeasurementEngineError()
}
