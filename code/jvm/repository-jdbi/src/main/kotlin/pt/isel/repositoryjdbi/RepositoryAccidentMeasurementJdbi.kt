package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.domain.Measurement
import pt.isel.repository.RepositoryAccidentMeasurement
import pt.isel.repositoryjdbi.mappers.MeasurementMapper

class RepositoryAccidentMeasurementJdbi(
    private val handle: Handle,
) : RepositoryAccidentMeasurement {
    override fun createMeasurement(
        analysisId: Int,
        evidenceId: Int,
        damageId: Int,
        refObjLengthCm: Double,
        refObjX1: Double,
        refObjY1: Double,
        refObjX2: Double,
        refObjY2: Double,
        damageAreaX1: Double,
        damageAreaY1: Double,
        damageAreaX2: Double,
        damageAreaY2: Double,
        calculatedHeightCm: Double,
        damageMinHeightCm: Double,
        damageMaxHeightCm: Double,
        scaleCmPerPixel: Double,
        confidence: Double,
        calibrationMethod: String,
        comparisonImagePath: String?,
    ): Measurement =
        handle.createUpdate(
            """
            INSERT INTO measurement (
                analysis_id,
                evidence_id,
                damage_id,
                ref_obj_length_cm,
                ref_obj_x1,
                ref_obj_y1,
                ref_obj_x2,
                ref_obj_y2,
                damage_area_x1,
                damage_area_y1,
                damage_area_x2,
                damage_area_y2,
                calculated_height_cm,
                damage_min_height_cm,
                damage_max_height_cm,
                scale_cm_per_pixel,
                confidence,
                calibration_method,
                comparison_image_path
            )
            VALUES (
                :analysis_id,
                :evidence_id,
                :damage_id,
                :ref_obj_length_cm,
                :ref_obj_x1,
                :ref_obj_y1,
                :ref_obj_x2,
                :ref_obj_y2,
                :damage_area_x1,
                :damage_area_y1,
                :damage_area_x2,
                :damage_area_y2,
                :calculated_height_cm,
                :damage_min_height_cm,
                :damage_max_height_cm,
                :scale_cm_per_pixel,
                :confidence,
                :calibration_method,
                :comparison_image_path
            )
            """,
        )
            .bind("analysis_id", analysisId)
            .bind("evidence_id", evidenceId)
            .bind("damage_id", damageId)
            .bind("ref_obj_length_cm", refObjLengthCm)
            .bind("ref_obj_x1", refObjX1)
            .bind("ref_obj_y1", refObjY1)
            .bind("ref_obj_x2", refObjX2)
            .bind("ref_obj_y2", refObjY2)
            .bind("damage_area_x1", damageAreaX1)
            .bind("damage_area_y1", damageAreaY1)
            .bind("damage_area_x2", damageAreaX2)
            .bind("damage_area_y2", damageAreaY2)
            .bind("calculated_height_cm", calculatedHeightCm)
            .bind("damage_min_height_cm", damageMinHeightCm)
            .bind("damage_max_height_cm", damageMaxHeightCm)
            .bind("scale_cm_per_pixel", scaleCmPerPixel)
            .bind("confidence", confidence)
            .bind("calibration_method", calibrationMethod)
            .bind("comparison_image_path", comparisonImagePath)
            .executeAndReturnGeneratedKeys(
                "measurement_id",
                "analysis_id",
                "evidence_id",
                "damage_id",
                "ref_obj_length_cm",
                "ref_obj_x1",
                "ref_obj_y1",
                "ref_obj_x2",
                "ref_obj_y2",
                "damage_area_x1",
                "damage_area_y1",
                "damage_area_x2",
                "damage_area_y2",
                "calculated_height_cm",
                "damage_min_height_cm",
                "damage_max_height_cm",
                "scale_cm_per_pixel",
                "confidence",
                "calibration_method",
                "comparison_image_path",
                "processed_at",
            )
            .map(MeasurementMapper())
            .one()

    override fun findMeasurementById(measurementId: Int): Measurement? =
        handle.createQuery("SELECT * FROM measurement WHERE measurement_id = :measurement_id")
            .bind("measurement_id", measurementId)
            .map(MeasurementMapper())
            .singleOrNull()

    override fun findMeasurementsByAnalysisId(analysisId: Int): List<Measurement> =
        handle.createQuery("SELECT * FROM measurement WHERE analysis_id = :analysis_id ORDER BY measurement_id ASC")
            .bind("analysis_id", analysisId)
            .map(MeasurementMapper())
            .list()

    override fun deleteMeasurementById(measurementId: Int): Int =
        handle.createUpdate("DELETE FROM measurement WHERE measurement_id = :measurement_id")
            .bind("measurement_id", measurementId)
            .execute()
}
