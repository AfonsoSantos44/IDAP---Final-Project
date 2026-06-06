package pt.isel.services

sealed class AccidentDataError { // Verificar
    data object CaseNotFound : AccidentDataError()

    data object UserNotFound : AccidentDataError()

    data object WeatherNotFound : AccidentDataError()

    data object SceneNotFound : AccidentDataError()

    data object VehicleNotFound : AccidentDataError()

    data object DamageNotFound : AccidentDataError()

    data object EvidenceNotFound : AccidentDataError()

    data object ImageEvidenceNotFound : AccidentDataError()

    data object AnalysisNotFound : AccidentDataError()

    data object AnalysisImageNotFound : AccidentDataError()

    data object MeasurementNotFound : AccidentDataError()

    data object DamageComparisonNotFound : AccidentDataError()

    data object AnalysisConclusionNotFound : AccidentDataError()

    data object ReportNotFound : AccidentDataError()

    data object DuplicateVehicle : AccidentDataError()

    data object InvalidAccidentData : AccidentDataError()

    data object RelatedResourceMismatch : AccidentDataError()

    data object MeasurementProcessingFailed : AccidentDataError()

    data object ReportGenerationFailed : AccidentDataError()

    data object ExternalDataUnavailable : AccidentDataError()
}
