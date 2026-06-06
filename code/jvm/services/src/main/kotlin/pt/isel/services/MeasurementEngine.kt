package pt.isel.services

import com.fasterxml.jackson.databind.ObjectMapper
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
    val primaryImagePath: String,
    val comparisonImagePath: String?,
    val outputDirectory: String,
    val knownTickDistanceCm: Double?,
    val outputBaseName: String,
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
    @Value("\${idap.measurement.timeout-seconds:30}")
    private val timeoutSeconds: Long,
    @Value("\${idap.measurement.tesseract-executable:}")
    private val tesseractExecutable: String,
) {
    fun measure(
        primaryImagePath: String,
        comparisonImagePath: String?,
        knownTickDistanceCm: Double?,
        primarySelection: DamageSelectionInput,
        primaryCalibration: RulerCalibrationInput?,
        comparisonSelection: DamageSelectionInput?,
        comparisonCalibration: RulerCalibrationInput?,
    ): Either<MeasurementEngineError, ComputedMeasurement> {
        val script =
            resolveRegularFile(scriptPath)
                ?: return failure(MeasurementEngineError.ScriptNotFound("Script not found: $scriptPath"))

        val primaryImage =
            resolveRegularFile(primaryImagePath)
                ?: return failure(MeasurementEngineError.InputImageNotFound("Primary image not found: $primaryImagePath"))
        val comparisonImage =
            comparisonImagePath?.let { path ->
                resolveRegularFile(path)
                    ?: return failure(MeasurementEngineError.InputImageNotFound("Comparison image not found: $path"))
            }

        val outputDir = Path.of(outputDirectory).absolute()
        Files.createDirectories(outputDir)

        val request =
            MeasurementEngineRequest(
                primaryImagePath = primaryImage.toString(),
                comparisonImagePath = comparisonImage?.toString(),
                outputDirectory = outputDir.toString(),
                knownTickDistanceCm = knownTickDistanceCm,
                outputBaseName = "measurement-${UUID.randomUUID()}",
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
