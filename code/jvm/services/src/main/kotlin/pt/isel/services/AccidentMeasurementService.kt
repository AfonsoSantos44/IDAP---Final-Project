package pt.isel.services

import org.slf4j.LoggerFactory
import org.springframework.stereotype.Service
import pt.isel.domain.Measurement
import pt.isel.repository.TransactionManager
import pt.isel.services.storage.ObjectStorage
import pt.isel.services.storage.StoredImage
import java.nio.file.Files
import java.nio.file.Path
import java.util.UUID

@Service
class AccidentMeasurementService(
    private val transactionManager: TransactionManager,
    private val measurementEngine: MeasurementEngine,
    private val objectStorage: ObjectStorage,
) {
    private val logger = LoggerFactory.getLogger(AccidentMeasurementService::class.java)

    fun createMeasurement(
        analysisId: Int,
        evidenceId: Int,
        damageId: Int,
        comparisonEvidenceId: Int?,
        knownTickDistanceCm: Double?,
        primarySelection: DamageSelectionInput,
        primaryCalibration: RulerCalibrationInput?,
        comparisonSelection: DamageSelectionInput?,
        comparisonCalibration: RulerCalibrationInput?,
    ): Either<AccidentDataError, Measurement> {
        if (knownTickDistanceCm != null && !isValidPositiveNumber(knownTickDistanceCm)) {
            return failure(AccidentDataError.InvalidAccidentData)
        }
        if (comparisonEvidenceId != null && comparisonSelection == null) {
            return failure(AccidentDataError.InvalidAccidentData)
        }
        if (comparisonEvidenceId == null && (comparisonSelection != null || comparisonCalibration != null)) {
            return failure(AccidentDataError.InvalidAccidentData)
        }

        val input =
            when (
                val result =
                    transactionManager.run {
                        val analysis =
                            repoAccidentAnalysis.findAnalysisById(analysisId)
                                ?: return@run failure(AccidentDataError.AnalysisNotFound)

                        ensureEvidenceAndDamageBelongToCase(
                            caseId = analysis.caseId,
                            evidenceId = evidenceId,
                            damageId = damageId,
                        )?.let { return@run failure(it) }

                        val primaryImage =
                            repoAccidentEvidence.findImageEvidenceByEvidenceId(evidenceId)
                                ?: return@run failure(AccidentDataError.ImageEvidenceNotFound)
                        if (!primarySelection.isValidFor(primaryImage.width, primaryImage.height)) {
                            return@run failure(AccidentDataError.InvalidAccidentData)
                        }
                        if (!primaryCalibration.isValidFor(primaryImage.width, primaryImage.height)) {
                            return@run failure(AccidentDataError.InvalidAccidentData)
                        }

                        val comparisonImage =
                            comparisonEvidenceId?.let { id ->
                                val comparisonEvidence =
                                    repoAccidentEvidence.findEvidenceById(id)
                                        ?: return@run failure(AccidentDataError.EvidenceNotFound)
                                if (comparisonEvidence.caseId != analysis.caseId) {
                                    return@run failure(AccidentDataError.RelatedResourceMismatch)
                                }
                                repoAccidentEvidence.findImageEvidenceByEvidenceId(id)
                                    ?: return@run failure(AccidentDataError.ImageEvidenceNotFound)
                            }
                        if (comparisonImage != null) {
                            if (!comparisonSelection.isValidFor(comparisonImage.width, comparisonImage.height)) {
                                return@run failure(AccidentDataError.InvalidAccidentData)
                            }
                            if (!comparisonCalibration.isValidFor(comparisonImage.width, comparisonImage.height)) {
                                return@run failure(AccidentDataError.InvalidAccidentData)
                            }
                        }

                        success(
                            MeasurementProcessingInput(
                                primaryImageKey = primaryImage.filePath,
                                comparisonImageKey = comparisonImage?.filePath,
                            ),
                        )
                    }
            ) {
                is Success -> result.value
                is Failure -> return result
            }

        // The Python engine works on local files. Download the stored objects to temporary
        // files, run the engine, then push the generated annotated image back to storage.
        val primaryTempImage = downloadToTemp(input.primaryImageKey)
        val comparisonTempImage = input.comparisonImageKey?.let { downloadToTemp(it) }

        val engineResult =
            try {
                when (
                    val result =
                        measurementEngine.measure(
                            primaryImagePath = primaryTempImage.toString(),
                            comparisonImagePath = comparisonTempImage?.toString(),
                            knownTickDistanceCm = knownTickDistanceCm,
                            primarySelection = primarySelection,
                            primaryCalibration = primaryCalibration,
                            comparisonSelection = comparisonSelection,
                            comparisonCalibration = comparisonCalibration,
                        )
                ) {
                    is Success -> result.value
                    is Failure -> {
                        logger.warn(
                            "Measurement engine failed for analysis {}, evidence {}, damage {}: {}",
                            analysisId,
                            evidenceId,
                            damageId,
                            result.value.message(),
                        )
                        return failure(AccidentDataError.MeasurementProcessingFailed)
                    }
                }
            } finally {
                runCatching { Files.deleteIfExists(primaryTempImage) }
                comparisonTempImage?.let { runCatching { Files.deleteIfExists(it) } }
            }

        // Upload the annotated comparison image (a local file produced by the engine) to
        // storage and keep only its object key in the database.
        val comparisonImageKey =
            engineResult.comparisonImagePath?.let { localPath ->
                uploadGeneratedImage(analysisId, localPath)
            }

        return transactionManager.run {
            val analysis =
                repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            ensureEvidenceAndDamageBelongToCase(
                caseId = analysis.caseId,
                evidenceId = evidenceId,
                damageId = damageId,
            )?.let { return@run failure(it) }

            success(
                repoAccidentMeasurement.createMeasurement(
                    analysisId = analysisId,
                    evidenceId = evidenceId,
                    damageId = damageId,
                    refObjLengthCm = engineResult.refObjLengthCm,
                    refObjX1 = engineResult.refObjX1,
                    refObjY1 = engineResult.refObjY1,
                    refObjX2 = engineResult.refObjX2,
                    refObjY2 = engineResult.refObjY2,
                    damageAreaX1 = engineResult.damageAreaX1,
                    damageAreaY1 = engineResult.damageAreaY1,
                    damageAreaX2 = engineResult.damageAreaX2,
                    damageAreaY2 = engineResult.damageAreaY2,
                    calculatedHeightCm = engineResult.calculatedHeightCm,
                    damageMinHeightCm = engineResult.damageMinHeightCm,
                    damageMaxHeightCm = engineResult.damageMaxHeightCm,
                    scaleCmPerPixel = engineResult.scaleCmPerPixel,
                    confidence = engineResult.confidence,
                    calibrationMethod = engineResult.calibrationMethod,
                    comparisonImagePath = comparisonImageKey,
                ).also {
                    repoAccidentVehicleDamage.updateDamageHeight(damageId, engineResult.calculatedHeightCm)
                },
            )
        }
    }

    fun getMeasurementsByAnalysisId(analysisId: Int): Either<AccidentDataError, List<Measurement>> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            success(repoAccidentMeasurement.findMeasurementsByAnalysisId(analysisId))
        }

    fun getMeasurementById(measurementId: Int): Either<AccidentDataError, Measurement> =
        transactionManager.run {
            repoAccidentMeasurement.findMeasurementById(measurementId)?.let { success(it) }
                ?: failure(AccidentDataError.MeasurementNotFound)
        }

    fun deleteMeasurement(measurementId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentMeasurement.findMeasurementById(measurementId)
                ?: return@run failure(AccidentDataError.MeasurementNotFound)
            repoAccidentMeasurement.deleteMeasurementById(measurementId)
            success(Unit)
        }

    /** Fetch the annotated comparison image bytes for a measurement, if one was generated. */
    fun getComparisonImageContent(measurementId: Int): Either<AccidentDataError, StoredImage> {
        val measurement =
            when (val result = getMeasurementById(measurementId)) {
                is Success -> result.value
                is Failure -> return result
            }
        val key =
            measurement.comparisonImagePath
                ?: return failure(AccidentDataError.ImageEvidenceNotFound)
        return success(StoredImage(objectStorage.get(key), contentTypeForKey(key)))
    }

    private fun downloadToTemp(key: String): Path {
        val extension = key.substringAfterLast('.', "img")
        val tempFile = Files.createTempFile("idap-measure-", ".$extension")
        Files.write(tempFile, objectStorage.get(key))
        return tempFile
    }

    private fun uploadGeneratedImage(
        analysisId: Int,
        localPath: String,
    ): String? {
        val source = Path.of(localPath)
        if (!Files.isRegularFile(source)) return null
        val extension = localPath.substringAfterLast('.', "png")
        val key = "measurements/$analysisId/${UUID.randomUUID()}.$extension"
        objectStorage.put(key, Files.readAllBytes(source), contentTypeForKey(key))
        runCatching { Files.deleteIfExists(source) }
        return key
    }

    private fun MeasurementEngineError.message(): String =
        when (this) {
            is MeasurementEngineError.ScriptNotFound -> detail
            is MeasurementEngineError.InputImageNotFound -> detail
            is MeasurementEngineError.Timeout -> "Measurement engine timed out"
            is MeasurementEngineError.ProcessingFailed -> detail
            is MeasurementEngineError.InvalidEngineResponse -> detail
        }

    private fun DamageSelectionInput?.isValidFor(
        width: Int,
        height: Int,
    ): Boolean {
        if (this == null) return false
        val values = listOf(x1, y1, x2, y2)
        if (values.any { !isValidNumber(it) }) return false

        val left = minOf(x1, x2)
        val right = maxOf(x1, x2)
        val top = minOf(y1, y2)
        val bottom = maxOf(y1, y2)
        return left >= 0.0 && top >= 0.0 && right < width && bottom < height
    }

    private fun RulerCalibrationInput?.isValidFor(
        width: Int,
        height: Int,
    ): Boolean {
        if (this == null) return true
        if (referencePoints.size == 1) return false
        if (referencePoints.isNotEmpty() && referencePoints.map { it.valueCm }.distinct().size < 2) return false
        if (
            referencePoints.any {
                !isValidNumber(it.x) ||
                    !isValidNumber(it.y) ||
                    !isValidNumber(it.valueCm) ||
                    it.x < 0.0 ||
                    it.y < 0.0 ||
                    it.x >= width ||
                    it.y >= height
            }
        ) {
            return false
        }
        return rulerRegion?.let {
            it.isValidFor(width, height) &&
                it.x1 != it.x2 &&
                it.y1 != it.y2
        } ?: true
    }
}
