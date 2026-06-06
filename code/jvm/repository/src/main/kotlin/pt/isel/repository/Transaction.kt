package pt.isel.repository

interface Transaction {
    val repoUsers: RepositoryUser
    val repoCases: RepositoryCase
    val repoAccidentEnvironment: RepositoryAccidentEnvironment
    val repoAccidentVehicleDamage: RepositoryAccidentVehicleDamage
    val repoAccidentEvidence: RepositoryAccidentEvidence
    val repoAccidentAnalysis: RepositoryAccidentAnalysis
    val repoAccidentMeasurement: RepositoryAccidentMeasurement
    val repoAccidentDamageComparison: RepositoryAccidentDamageComparison
    val repoAccidentConclusionReport: RepositoryAccidentConclusionReport

    fun rollback()
}
