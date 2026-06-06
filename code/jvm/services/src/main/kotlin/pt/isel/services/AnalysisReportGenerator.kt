package pt.isel.services

import com.fasterxml.jackson.databind.ObjectMapper
import org.springframework.beans.factory.annotation.Value
import org.springframework.stereotype.Service
import pt.isel.domain.AccidentCase
import pt.isel.domain.AccidentScene
import pt.isel.domain.Analysis
import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.AnalysisImage
import pt.isel.domain.Damage
import pt.isel.domain.DamageComparison
import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence
import pt.isel.domain.Measurement
import pt.isel.domain.Vehicle
import pt.isel.domain.WeatherConditions
import java.nio.file.Files
import java.nio.file.Path
import java.time.Instant
import java.util.UUID

@Service
class AnalysisReportGenerator(
    private val objectMapper: ObjectMapper,
    @Value("\${idap.report.output-dir:build/idap-reports}")
    private val outputDirectory: String,
) {
    fun generate(
        snapshot: AnalysisReportSnapshot,
        requestedFilePath: String?,
    ): Either<ReportGenerationError, String> =
        try {
            val outputPath = resolveOutputPath(snapshot.analysis.analysisId, requestedFilePath)
            Files.createDirectories(outputPath.parent)
            objectMapper
                .writerWithDefaultPrettyPrinter()
                .writeValue(outputPath.toFile(), snapshot.toDocument())
            success(outputPath.toString())
        } catch (ex: Exception) {
            failure(ReportGenerationError(ex.message ?: "Report generation failed"))
        }

    private fun resolveOutputPath(
        analysisId: Int,
        requestedFilePath: String?,
    ): Path {
        val baseDirectory = Path.of(outputDirectory).toAbsolutePath().normalize()
        val target =
            requestedFilePath
                ?.let { Path.of(it) }
                ?.let { path -> if (path.isAbsolute) path else baseDirectory.resolve(path) }
                ?: baseDirectory.resolve("analysis-$analysisId-report-${UUID.randomUUID()}.json")

        return withJsonExtension(target.toAbsolutePath().normalize())
    }

    private fun withJsonExtension(path: Path): Path {
        val fileName = path.fileName.toString()
        if (fileName.endsWith(".json", ignoreCase = true)) return path

        val baseName = fileName.substringBeforeLast('.', fileName)
        return path.resolveSibling("$baseName.json")
    }

    private fun AnalysisReportSnapshot.toDocument(): AnalysisReportDocument =
        AnalysisReportDocument(
            schemaVersion = "1.0",
            generatedAt = Instant.now(),
            case = case,
            environment =
                ReportEnvironmentData(
                    weather = weather,
                    scene = scene,
                ),
            analysis = analysis,
            vehicles = vehicles,
            evidence = evidence,
            analysisImages = analysisImages,
            measurements = measurements,
            damageComparisons = damageComparisons,
            conclusion = conclusion,
        )
}

data class AnalysisReportDocument(
    val schemaVersion: String,
    val generatedAt: Instant,
    val case: AccidentCase,
    val environment: ReportEnvironmentData,
    val analysis: Analysis,
    val vehicles: List<VehicleReportData>,
    val evidence: List<EvidenceReportData>,
    val analysisImages: List<AnalysisImageReportData>,
    val measurements: List<Measurement>,
    val damageComparisons: List<DamageComparison>,
    val conclusion: AnalysisConclusion?,
)

data class ReportEnvironmentData(
    val weather: WeatherConditions?,
    val scene: AccidentScene?,
)

data class AnalysisReportSnapshot(
    val case: AccidentCase,
    val weather: WeatherConditions?,
    val scene: AccidentScene?,
    val analysis: Analysis,
    val vehicles: List<VehicleReportData>,
    val evidence: List<EvidenceReportData>,
    val analysisImages: List<AnalysisImageReportData>,
    val measurements: List<Measurement>,
    val damageComparisons: List<DamageComparison>,
    val conclusion: AnalysisConclusion?,
)

data class VehicleReportData(
    val vehicle: Vehicle,
    val damages: List<Damage>,
)

data class EvidenceReportData(
    val evidence: Evidence,
    val image: ImageEvidence?,
)

data class AnalysisImageReportData(
    val analysisImage: AnalysisImage,
    val evidence: Evidence?,
    val image: ImageEvidence?,
)

data class ReportGenerationError(
    val detail: String,
)
