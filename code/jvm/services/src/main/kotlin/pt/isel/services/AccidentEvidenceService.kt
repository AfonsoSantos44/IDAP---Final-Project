package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence
import pt.isel.repository.TransactionManager

@Service
class AccidentEvidenceService(
    private val transactionManager: TransactionManager,
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

    fun upsertImageEvidence(
        evidenceId: Int,
        filePath: String,
        width: Int,
        height: Int,
        metadata: String?,
    ): Either<AccidentDataError, ImageEvidence> {
        val normalizedFilePath =
            normalizeRequiredText(filePath, MAX_FILE_PATH)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        if (width <= 0 || height <= 0) return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(metadata, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)

            val currentImage = repoAccidentEvidence.findImageEvidenceByEvidenceId(evidenceId)
            success(
                if (currentImage == null) {
                    repoAccidentEvidence.createImageEvidence(
                        evidenceId = evidenceId,
                        filePath = normalizedFilePath,
                        width = width,
                        height = height,
                        metadata = normalizeOptionalText(metadata),
                    )
                } else {
                    repoAccidentEvidence.updateImageEvidence(
                        imageEvidenceId = currentImage.imageEvidenceId,
                        filePath = normalizedFilePath,
                        width = width,
                        height = height,
                        metadata = normalizeOptionalText(metadata),
                    ) ?: currentImage
                },
            )
        }
    }

    fun getImageEvidenceByEvidenceId(evidenceId: Int): Either<AccidentDataError, ImageEvidence> =
        transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)
            repoAccidentEvidence.findImageEvidenceByEvidenceId(evidenceId)?.let { success(it) }
                ?: failure(AccidentDataError.ImageEvidenceNotFound)
        }

    fun deleteImageEvidence(evidenceId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentEvidence.findEvidenceById(evidenceId) ?: return@run failure(AccidentDataError.EvidenceNotFound)
            if (repoAccidentEvidence.deleteImageEvidenceByEvidenceId(evidenceId) == 0) {
                return@run failure(AccidentDataError.ImageEvidenceNotFound)
            }
            success(Unit)
        }
}
