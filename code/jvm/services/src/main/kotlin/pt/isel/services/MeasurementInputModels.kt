package pt.isel.services

data class DamageSelectionInput(
    val x1: Double,
    val y1: Double,
    val x2: Double,
    val y2: Double,
)

data class RulerReferencePointInput(
    val x: Double,
    val y: Double,
    val valueCm: Double,
)

data class RulerCalibrationInput(
    val referencePoints: List<RulerReferencePointInput> = emptyList(),
    val rulerRegion: DamageSelectionInput? = null,
)
