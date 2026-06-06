package pt.isel.services

import org.springframework.stereotype.Service
import pt.isel.domain.DamageComparison
import pt.isel.repository.TransactionManager

@Service
class AccidentDamageComparisonService(
    private val transactionManager: TransactionManager,
) {
    fun createDamageComparison(
        analysisId: Int,
        damageSourceId: Int,
        damageTargetId: Int,
        compatibilityStatus: String,
        notes: String?,
    ): Either<AccidentDataError, DamageComparison> {
        val normalizedCompatibilityStatus =
            normalizeRequiredText(compatibilityStatus, MAX_STATUS_TEXT)
                ?: return failure(AccidentDataError.InvalidAccidentData)
        if (!isValidOptionalText(notes, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)
        if (damageSourceId == damageTargetId) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val analysis =
                repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            ensureDamageBelongsToCase(analysis.caseId, damageSourceId)?.let { return@run failure(it) }
            ensureDamageBelongsToCase(analysis.caseId, damageTargetId)?.let { return@run failure(it) }

            success(
                repoAccidentDamageComparison.createDamageComparison(
                    analysisId = analysisId,
                    damageSourceId = damageSourceId,
                    damageTargetId = damageTargetId,
                    compatibilityStatus = normalizedCompatibilityStatus,
                    notes = normalizeOptionalText(notes),
                ),
            )
        }
    }

    fun getDamageComparisonsByAnalysisId(analysisId: Int): Either<AccidentDataError, List<DamageComparison>> =
        transactionManager.run {
            repoAccidentAnalysis.findAnalysisById(analysisId) ?: return@run failure(AccidentDataError.AnalysisNotFound)
            success(repoAccidentDamageComparison.findDamageComparisonsByAnalysisId(analysisId))
        }

    fun getDamageComparisonById(comparisonId: Int): Either<AccidentDataError, DamageComparison> =
        transactionManager.run {
            repoAccidentDamageComparison.findDamageComparisonById(comparisonId)?.let { success(it) }
                ?: failure(AccidentDataError.DamageComparisonNotFound)
        }

    fun updateDamageComparison(
        comparisonId: Int,
        damageSourceId: Int?,
        damageTargetId: Int?,
        compatibilityStatus: String?,
        notes: String?,
    ): Either<AccidentDataError, DamageComparison> {
        if (!isValidOptionalText(compatibilityStatus, MAX_STATUS_TEXT)) {
            return failure(AccidentDataError.InvalidAccidentData)
        }
        if (!isValidOptionalText(notes, MAX_LONG_TEXT)) return failure(AccidentDataError.InvalidAccidentData)

        return transactionManager.run {
            val currentComparison =
                repoAccidentDamageComparison.findDamageComparisonById(comparisonId)
                    ?: return@run failure(AccidentDataError.DamageComparisonNotFound)
            val analysis =
                repoAccidentAnalysis.findAnalysisById(currentComparison.analysisId)
                    ?: return@run failure(AccidentDataError.AnalysisNotFound)

            val nextDamageSourceId = damageSourceId ?: currentComparison.damageSourceId
            val nextDamageTargetId = damageTargetId ?: currentComparison.damageTargetId
            val nextCompatibilityStatus =
                normalizeOptionalText(compatibilityStatus) ?: currentComparison.compatibilityStatus
            val nextNotes = if (notes == null) currentComparison.notes else normalizeOptionalText(notes)

            if (nextDamageSourceId == nextDamageTargetId) return@run failure(AccidentDataError.InvalidAccidentData)
            ensureDamageBelongsToCase(analysis.caseId, nextDamageSourceId)?.let { return@run failure(it) }
            ensureDamageBelongsToCase(analysis.caseId, nextDamageTargetId)?.let { return@run failure(it) }

            success(
                repoAccidentDamageComparison.updateDamageComparison(
                    comparisonId = comparisonId,
                    damageSourceId = nextDamageSourceId,
                    damageTargetId = nextDamageTargetId,
                    compatibilityStatus = nextCompatibilityStatus,
                    notes = nextNotes,
                ) ?: currentComparison,
            )
        }
    }

    fun deleteDamageComparison(comparisonId: Int): Either<AccidentDataError, Unit> =
        transactionManager.run {
            repoAccidentDamageComparison.findDamageComparisonById(comparisonId)
                ?: return@run failure(AccidentDataError.DamageComparisonNotFound)
            repoAccidentDamageComparison.deleteDamageComparisonById(comparisonId)
            success(Unit)
        }
}
