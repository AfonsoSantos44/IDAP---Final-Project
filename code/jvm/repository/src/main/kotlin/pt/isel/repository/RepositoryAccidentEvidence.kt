package pt.isel.repository

import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence

interface RepositoryAccidentEvidence {
    fun createEvidence(
        caseId: Int,
        evidenceType: String,
        evidenceDescription: String,
        uploadedBy: Int,
    ): Evidence

    fun findEvidenceById(evidenceId: Int): Evidence?

    fun findEvidenceByCaseId(caseId: Int): List<Evidence>

    fun updateEvidence(
        evidenceId: Int,
        evidenceType: String,
        evidenceDescription: String,
    ): Evidence?

    fun deleteEvidenceById(evidenceId: Int): Int

    fun createImageEvidence(
        evidenceId: Int,
        filePath: String,
        width: Int,
        height: Int,
        metadata: String?,
    ): ImageEvidence

    fun findImageEvidenceByEvidenceId(evidenceId: Int): ImageEvidence?

    fun updateImageEvidence(
        imageEvidenceId: Int,
        filePath: String,
        width: Int,
        height: Int,
        metadata: String?,
    ): ImageEvidence?

    fun deleteImageEvidenceByEvidenceId(evidenceId: Int): Int
}
