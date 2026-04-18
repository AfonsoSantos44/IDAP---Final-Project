package pt.isel.repository

interface Transaction {
    val repoUsers: RepositoryUser
    
    fun rollback()
}
