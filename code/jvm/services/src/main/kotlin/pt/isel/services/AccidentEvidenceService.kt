package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence
import pt.isel.repository.TransactionManager
import pt.isel.services.storage.ObjectStorage
import pt.isel.services.storage.StoredImage
import java.io.ByteArrayInputStream
import java.util.UUID
import javax.imageio.ImageIO

@Service
class AccidentEvidenceService(
    private val transactionManager: TransactionManager,
    private val objectStorage: ObjectStorage,
) {
    fun createEvidence(
        caseId: Int,
        evidenceType: String,
        evidenceDescription: String,
        uploadedBy: Int,
    ): Either<AccidentDataError, Evidence> {
        val normalizedType =
            normalizeRequiredText(evidenceType, MAX_EVIDENCE_TYPE)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val normalizedDescription =
            normalizeRequiredText(evidenceDescription, MAX_LONG_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            repoUsers.findById(uploadedBy) ?: return@run failure(AccidentDataError.UserNotFound)

            success(
                repoAccidentEvidence.createEvidence(
                    caseId = caseId,
                    evidenceType = normalizedType,
                    evidenceDescription = normalizedDescription,
                    uploadedBy = uploadedBy,
                ),
            )
        }
    }

    fun getEvidenceByCaseId(caseId: Int): Either<AccidentDataError, List<Evidence>> =
        transactionManager.run {
            repoCases.findById(caseId) ?: return@run failure(AccidentDataError.CaseNotFound)
            success(repoAccidentEvidence.findEvidenceByCaseId(caseId))
        }

    fun getEvidenceById(evidenceId: Int): Either<AccidentDataError, Evidence> =
        transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId)?.let { success(it) }
                ?: failure(AccidentDataError.EvidenceNotFound)
        }

    fun updateEvidence(
        evidenceId: Int,
        evidenceType: String?,
        evidenceDescription: String?,
    ): Either<AccidentDataError, Evidence> {
        if (!isValidOptionalText(evidenceType, MAX_EVIDENCE_TYPE)) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(evidenceDescription, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val currentEvidence =
                repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)

            success(
                repoAccidentEvidence.updateEvidence(
                    evidenceId = evidenceId,
                    evidenceType = normalizeOptionalText(evidenceType) ?: currentEvidence.evidenceType,
                    evidenceDescription = normalizeOptionalText(evidenceDescription) ?: currentEvidence.evidenceDescription,
                ) ?: currentEvidence,
            )
        }
    }

    fun deleteEvidence(evidenceId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)
            repoAccidentEvidence.deleteEvidenceById(evidenceId)
            success(Unit)
        }

    /**
     * Store the uploaded image [bytes] in object storage and persist its metadata.
     *
     * The image dimensions are read from the bytes; the relational row keeps only the
     * generated object [key][ImageEvidence.filePath], never the binary content.
     */
    fun uploadImageEvidence(
        evidenceId: Int,
        bytes: ByteArray,
        contentType: String?,
        metadata: String?,
    ): Either<AccidentDataError, ImageEvidence> {
        if (bytes.isEmpty()) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(metadata, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        val dimensions =
            readImageDimensions(bytes)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        val extension = imageExtension(contentType, dimensions.formatName)

        return transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)

            val currentImage = repoAccidentEvidence.findImageEvidenceByEvidenceId(evidenceId)
            val key = "evidence/$evidenceId/${UUID.randomUUID()}.$extension"

            objectStorage.put(key, bytes, contentType?.takeIf { it.isNotBlank() } ?: "application/octet-stream")

            val saved =
                if (currentImage == null) {
                    repoAccidentEvidence.createImageEvidence(
                        evidenceId = evidenceId,
                        filePath = key,
                        width = dimensions.width,
                        height = dimensions.height,
                        metadata = normalizeOptionalText(metadata),
                    )
                } else {
                    repoAccidentEvidence.updateImageEvidence(
                        imageEvidenceId = currentImage.imageEvidenceId,
                        filePath = key,
                        width = dimensions.width,
                        height = dimensions.height,
                        metadata = normalizeOptionalText(metadata),
                    ) ?: currentImage
                }

            if (currentImage != null && currentImage.filePath != key) {
                runCatching { objectStorage.delete(currentImage.filePath) }
            }

            success(saved)
        }
    }

    fun getImageEvidenceByEvidenceId(evidenceId: Int): Either<AccidentDataError, ImageEvidence> =
        transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)
            repoAccidentEvidence.findImageEvidenceByEvidenceId(evidenceId)?.let { success(it) }
                ?: failure(AccidentDataError.ImageEvidenceNotFound)
        }

    /** Fetch the raw image bytes from object storage so they can be streamed to a client. */
    fun getImageContent(evidenceId: Int): Either<AccidentDataError, StoredImage> {
        val image =
            when (val result = getImageEvidenceByEvidenceId(evidenceId)) {
                is Success -> result.value
                is Failure -> return result
            }
        return success(StoredImage(objectStorage.get(image.filePath), contentTypeForKey(image.filePath)))
    }

    fun deleteImageEvidence(evidenceId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)
            val image =
                repoAccidentEvidence.findImageEvidenceByEvidenceId(evidenceId)
                    ?: return@run failure(AccidentDataError.ImageEvidenceNotFound)
            repoAccidentEvidence.deleteImageEvidenceByEvidenceId(evidenceId)
            runCatching { objectStorage.delete(image.filePath) }
            success(Unit)
        }

    private data class ImageDimensions(
        val width: Int,
        val height: Int,
        val formatName: String?,
    )

    private fun readImageDimensions(bytes: ByteArray): ImageDimensions? =
        ImageIO.createImageInputStream(ByteArrayInputStream(bytes)).use { stream ->
            val readers = ImageIO.getImageReaders(stream)
            if (!readers.hasNext()) return null
            val reader = readers.next()
            reader.input = stream
            try {
                ImageDimensions(reader.getWidth(0), reader.getHeight(0), reader.formatName?.lowercase())
            } catch (ex: Exception) {
                null
            } finally {
                reader.dispose()
            }
        }
}

internal fun imageExtension(
    contentType: String?,
    formatName: String?,
): String =
    when {
        contentType == "image/png" || formatName == "png" -> "png"
        contentType == "image/jpeg" || formatName == "jpg" || formatName == "jpeg" -> "jpg"
        contentType == "image/webp" || formatName == "webp" -> "webp"
        else -> formatName ?: "bin"
    }

internal fun contentTypeForKey(key: String): String =
    when (key.substringAfterLast('.', "").lowercase()) {
        "png" -> "image/png"
        "jpg", "jpeg" -> "image/jpeg"
        "webp" -> "image/webp"
        else -> "application/octet-stream"
    }
