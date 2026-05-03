package pt.isel

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.repository.TransactionManager
import pt.isel.repositoryjdbi.TransactionManagerJdbi
import pt.isel.repositoryjdbi.mappers.TokenMapper
import pt.isel.repositoryjdbi.mappers.UserMapper
import java.time.Clock
import javax.sql.DataSource

@Configuration
class AppConfig {
    @Bean
    fun jdbi(dataSource: DataSource): Jdbi =
        Jdbi.create(dataSource)
            .installPlugin(KotlinPlugin())
            .registerRowMapper(UserMapper())
            .registerRowMapper(TokenMapper())

    @Bean
    fun idapTransactionManager(jdbi: Jdbi): TransactionManager =
        TransactionManagerJdbi(jdbi)

    @Bean
    fun passwordEncoder(): PasswordEncoder =
        BCryptPasswordEncoder()

    @Bean
    fun clock(): Clock =
        Clock.systemUTC()
}