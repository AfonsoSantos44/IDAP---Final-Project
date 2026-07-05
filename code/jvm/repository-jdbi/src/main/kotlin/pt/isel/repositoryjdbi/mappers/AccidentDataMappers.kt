package pt.isel.repositoryjdbi.mappers

import org.jdbi.v3.core.mapper.RowMapper
import org.jdbi.v3.core.statement.StatementContext
import pt.isel.domain.AccidentScene
import pt.isel.domain.Analysis
import pt.isel.domain.AnalysisConclusion
import pt.isel.domain.AnalysisImage
import pt.isel.domain.Damage
import pt.isel.domain.DamageComparison
import pt.isel.domain.Evidence
import pt.isel.domain.ImageEvidence
import pt.isel.domain.Measurement
import pt.isel.domain.Report
import pt.isel.domain.Vehicle
import pt.isel.domain.WeatherConditions
import java.sql.ResultSet

class WeatherConditionsMapper : RowMapper<WeatherConditions> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): WeatherConditions =
        WeatherConditions(
            weatherId = rs.getInt("weather_id"),
            caseId = rs.getInt("case_id"),
            conditionType = rs.getString("condition_type"),
            temperature = rs.getNullableDouble("temperature"),
            visibility = rs.getNullableDouble("visibility"),
            precipitation = rs.getString("precipitation"),
        )
}

class AccidentSceneMapper : RowMapper<AccidentScene> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): AccidentScene =
        AccidentScene(
            sceneId = rs.getInt("scene_id"),
            caseId = rs.getInt("case_id"),
            latitude = rs.getDouble("latitude"),
            longitude = rs.getDouble("longitude"),
            terrainInclination = rs.getDouble("terrain_inclination"),
            roadGradient = rs.getDouble("road_gradient"),
            roadType = rs.getString("road_type"),
            spatialDescription = rs.getString("spatial_description"),
            vehiclePositioningNotes = rs.getString("vehicle_positioning_notes"),
        )
}

class VehicleMapper : RowMapper<Vehicle> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Vehicle =
        Vehicle(
            vehicleId = rs.getInt("vehicle_id"),
            caseId = rs.getInt("case_id"),
            brand = rs.getString("brand"),
            model = rs.getString("model"),
            yearOfFabrication = rs.getInt("year_of_fabrication"),
            licensePlate = rs.getString("license_plate"),
            role = rs.getString("role"),
        )
}

class DamageMapper : RowMapper<Damage> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Damage =
        Damage(
            damageId = rs.getInt("damage_id"),
            vehicleId = rs.getInt("vehicle_id"),
            contactZone = rs.getString("contact_zone"),
            deformationType = rs.getString("deformation_type"),
            direction = rs.getString("direction"),
            heightCm = rs.getNullableDouble("height_cm"),
            damageDescription = rs.getString("damage_description"),
        )
}

class EvidenceMapper : RowMapper<Evidence> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Evidence =
        Evidence(
            evidenceId = rs.getInt("evidence_id"),
            caseId = rs.getInt("case_id"),
            evidenceType = rs.getString("evidence_type"),
            evidenceDescription = rs.getString("evidence_description"),
            uploadedBy = rs.getInt("uploaded_by"),
            uploadedAt = rs.getTimestamp("uploaded_at").toInstant(),
        )
}

class ImageEvidenceMapper : RowMapper<ImageEvidence> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): ImageEvidence =
        ImageEvidence(
            imageEvidenceId = rs.getInt("image_evidence_id"),
            evidenceId = rs.getInt("evidence_id"),
            vehicleId = rs.getInt("vehicle_id"),
            filePath = rs.getString("file_path"),
            width = rs.getInt("width"),
            height = rs.getInt("height"),
            metadata = rs.getString("metadata"),
        )
}

class AnalysisMapper : RowMapper<Analysis> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Analysis =
        Analysis(
            analysisId = rs.getInt("analysis_id"),
            caseId = rs.getInt("case_id"),
            analystId = rs.getInt("analyst_id"),
            createdAt = rs.getTimestamp("created_at").toInstant(),
        )
}

class AnalysisImageMapper : RowMapper<AnalysisImage> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): AnalysisImage =
        AnalysisImage(
            analysisId = rs.getInt("analysis_id"),
            evidenceId = rs.getInt("evidence_id"),
            purpose = rs.getString("purpose"),
        )
}

class MeasurementMapper : RowMapper<Measurement> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Measurement =
        Measurement(
            measurementId = rs.getInt("measurement_id"),
            analysisId = rs.getInt("analysis_id"),
            evidenceId = rs.getInt("evidence_id"),
            damageId = rs.getInt("damage_id"),
            refObjLengthCm = rs.getDouble("ref_obj_length_cm"),
            refObjX1 = rs.getDouble("ref_obj_x1"),
            refObjY1 = rs.getDouble("ref_obj_y1"),
            refObjX2 = rs.getDouble("ref_obj_x2"),
            refObjY2 = rs.getDouble("ref_obj_y2"),
            damageAreaX1 = rs.getDouble("damage_area_x1"),
            damageAreaY1 = rs.getDouble("damage_area_y1"),
            damageAreaX2 = rs.getDouble("damage_area_x2"),
            damageAreaY2 = rs.getDouble("damage_area_y2"),
            calculatedHeightCm = rs.getDouble("calculated_height_cm"),
            damageMinHeightCm = rs.getDouble("damage_min_height_cm"),
            damageMaxHeightCm = rs.getDouble("damage_max_height_cm"),
            scaleCmPerPixel = rs.getDouble("scale_cm_per_pixel"),
            confidence = rs.getDouble("confidence"),
            calibrationMethod = rs.getString("calibration_method"),
            comparisonImagePath = rs.getString("comparison_image_path"),
            processedAt = rs.getTimestamp("processed_at").toInstant(),
        )
}

class DamageComparisonMapper : RowMapper<DamageComparison> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): DamageComparison =
        DamageComparison(
            comparisonId = rs.getInt("comparison_id"),
            analysisId = rs.getInt("analysis_id"),
            damageSourceId = rs.getInt("damage_source_id"),
            damageTargetId = rs.getInt("damage_target_id"),
            compatibilityStatus = rs.getString("compatibility_status"),
            notes = rs.getString("notes"),
        )
}

class AnalysisConclusionMapper : RowMapper<AnalysisConclusion> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): AnalysisConclusion =
        AnalysisConclusion(
            conclusionId = rs.getInt("conclusion_id"),
            analysisId = rs.getInt("analysis_id"),
            compatibilityResult = rs.getString("compatibility_result"),
            justification = rs.getString("justification"),
        )
}

class ReportMapper : RowMapper<Report> {
    override fun map(
        rs: ResultSet,
        ctx: StatementContext,
    ): Report =
        Report(
            reportId = rs.getInt("report_id"),
            analysisId = rs.getInt("analysis_id"),
            generatedAt = rs.getTimestamp("generated_at").toInstant(),
            filePath = rs.getString("file_path"),
        )
}

private fun ResultSet.getNullableDouble(columnLabel: String): Double? {
    val value = getDouble(columnLabel)
    return if (wasNull()) null else value
}
