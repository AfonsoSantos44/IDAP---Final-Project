package pt.isel.domain

data class ImageEvidence(
    val imageEvidenceId: Int,
    val evidenceId: Int,
    val vehicleId: Int,
    val filePath: String,
    val width: Int,
    val height: Int,
    val metadata: String?,
)
