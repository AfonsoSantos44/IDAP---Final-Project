package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence
import pt.isel.repository.RepositoryAccidentEvidence
import pt.isel.repositoryjdbi.mappers.EvidenceMapper
import pt.isel.repositoryjdbi.mappers.ImageEvidenceMapper

class RepositoryAccidentEvidenceJdbi(
    private val handle: Handle,
) : RepositoryAccidentEvidence {
    override fun createEvidence(
        caseId: Int,
        evidenceType: String,
        evidenceDescription: String,
        uploadedBy: Int,
    ): Evidence =
        handle.createUpdate(
            """
            INSERT INTO evidence (case_id, evidence_type, evidence_description, uploaded_by)
            VALUES (:case_id, :evidence_type, :evidence_description, :uploaded_by)
            """,
        )
            .bind("case_id", caseId)
            .bind("evidence_type", evidenceType)
            .bind("evidence_description", evidenceDescription)
            .bind("uploaded_by", uploadedBy)
            .executeAndReturnGeneratedKeys(
                "evidence_id",
                "case_id",
                "evidence_type",
                "evidence_description",
                "uploaded_by",
                "uploaded_at",
            )
            .map(EvidenceMapper())
            .one()

    override fun findEvidenceById(evidenceId: Int): Evidence? =
        handle.createQuery("SELECT * FROM evidence WHERE evidence_id = :evidence_id")
            .bind("evidence_id", evidenceId)
            .map(EvidenceMapper())
            .singleOrNull()

    override fun findEvidenceByCaseId(caseId: Int): List<Evidence> =
        handle.createQuery("SELECT * FROM evidence WHERE case_id = :case_id ORDER BY uploaded_at DESC, evidence_id DESC")
            .bind("case_id", caseId)
            .map(EvidenceMapper())
            .list()

    override fun updateEvidence(
        evidenceId: Int,
        evidenceType: String,
        evidenceDescription: String,
    ): Evidence? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE evidence
                SET evidence_type = :evidence_type,
                    evidence_description = :evidence_description
                WHERE evidence_id = :evidence_id
                """,
            )
                .bind("evidence_id", evidenceId)
                .bind("evidence_type", evidenceType)
                .bind("evidence_description", evidenceDescription)
                .execute()

        return if (rowsUpdated == 0) null else findEvidenceById(evidenceId)
    }

    override fun deleteEvidenceById(evidenceId: Int): Int =
        handle.createUpdate("DELETE FROM evidence WHERE evidence_id = :evidence_id")
            .bind("evidence_id", evidenceId)
            .execute()

    override fun createImageEvidence(
        evidenceId: Int,
        vehicleId: Int,
        filePath: String,
        width: Int,
        height: Int,
        metadata: String?,
    ): ImageEvidence =
        handle.createUpdate(
            """
            INSERT INTO image_evidence (evidence_id, vehicle_id, file_path, width, height, metadata)
            VALUES (:evidence_id, :vehicle_id, :file_path, :width, :height, :metadata)
            """,
        )
            .bind("evidence_id", evidenceId)
            .bind("vehicle_id", vehicleId)
            .bind("file_path", filePath)
            .bind("width", width)
            .bind("height", height)
            .bind("metadata", metadata)
            .executeAndReturnGeneratedKeys(
                "image_evidence_id",
                "evidence_id",
                "vehicle_id",
                "file_path",
                "width",
                "height",
                "metadata",
            )
            .map(ImageEvidenceMapper())
            .one()

    override fun findImageEvidenceByEvidenceId(evidenceId: Int): ImageEvidence? =
        handle.createQuery("SELECT * FROM image_evidence WHERE evidence_id = :evidence_id")
            .bind("evidence_id", evidenceId)
            .map(ImageEvidenceMapper())
            .singleOrNull()

    override fun updateImageEvidence(
        imageEvidenceId: Int,
        vehicleId: Int,
        filePath: String,
        width: Int,
        height: Int,
        metadata: String?,
    ): ImageEvidence? {
        val rowsUpdated =
            handle.createUpdate(
                """
                UPDATE image_evidence
                SET vehicle_id = :vehicle_id,
                    file_path = :file_path,
                    width = :width,
                    height = :height,
                    metadata = :metadata
                WHERE image_evidence_id = :image_evidence_id
                """,
            )
                .bind("image_evidence_id", imageEvidenceId)
                .bind("vehicle_id", vehicleId)
                .bind("file_path", filePath)
                .bind("width", width)
                .bind("height", height)
                .bind("metadata", metadata)
                .execute()

        return if (rowsUpdated == 0) null else findImageEvidenceById(imageEvidenceId)
    }

    override fun deleteImageEvidenceByEvidenceId(evidenceId: Int): Int =
        handle.createUpdate("DELETE FROM image_evidence WHERE evidence_id = :evidence_id")
            .bind("evidence_id", evidenceId)
            .execute()

    override fun findImageEvidenceById(imageEvidenceId: Int): ImageEvidence? =
        handle.createQuery("SELECT * FROM image_evidence WHERE image_evidence_id = :image_evidence_id")
            .bind("image_evidence_id", imageEvidenceId)
            .map(ImageEvidenceMapper())
            .singleOrNull()
}
