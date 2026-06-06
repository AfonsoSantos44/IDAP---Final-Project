package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.repository.Transaction

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)
    override val repoCases = RepositoryCaseJdbi(handle)
    override val repoAccidentEnvironment = RepositoryAccidentEnvironmentJdbi(handle)
    override val repoAccidentVehicleDamage = RepositoryAccidentVehicleDamageJdbi(handle)
    override val repoAccidentEvidence = RepositoryAccidentEvidenceJdbi(handle)
    override val repoAccidentAnalysis = RepositoryAccidentAnalysisJdbi(handle)
    override val repoAccidentMeasurement = RepositoryAccidentMeasurementJdbi(handle)
    override val repoAccidentDamageComparison = RepositoryAccidentDamageComparisonJdbi(handle)
    override val repoAccidentConclusionReport = RepositoryAccidentConclusionReportJdbi(handle)

    override fun rollback() {
        handle.rollback()
    }
}
