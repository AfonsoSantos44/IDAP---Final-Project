package pt.isel.repositoryjdbi

import org.jdbi.v3.core.Handle
import pt.isel.repository.Transaction

class TransactionJdbi(
    private val handle: Handle,
) : Transaction {
    override val repoUsers = RepositoryUserJdbi(handle)
    // Add the others 

    override fun rollback() {
        handle.rollback()
    }
}
