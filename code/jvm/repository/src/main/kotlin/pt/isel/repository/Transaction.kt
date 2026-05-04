package pt.isel.repository

interface Transaction {
    val repoUsers: RepositoryUser
    val repoCases: RepositoryCase
    
    fun rollback()
}
