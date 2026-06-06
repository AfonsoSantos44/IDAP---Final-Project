package pt.isel

import org.jdbi.v3.core.Jdbi
import org.jdbi.v3.core.kotlin.KotlinPlugin
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder
import org.springframework.security.crypto.password.PasswordEncoder
import pt.isel.repository.TransactionManager
import pt.isel.repositoryjdbi.TransactionManagerJdbi
import pt.isel.repositoryjdbi.mappers.AccidentSceneMapper
import pt.isel.repositoryjdbi.mappers.AccidentCaseMapper
import pt.isel.repositoryjdbi.mappers.AnalysisConclusionMapper
import pt.isel.repositoryjdbi.mappers.AnalysisImageMapper
import pt.isel.repositoryjdbi.mappers.AnalysisMapper
import pt.isel.repositoryjdbi.mappers.DamageComparisonMapper
import pt.isel.repositoryjdbi.mappers.DamageMapper
import pt.isel.repositoryjdbi.mappers.EvidenceMapper
import pt.isel.repositoryjdbi.mappers.ImageEvidenceMapper
import pt.isel.repositoryjdbi.mappers.MeasurementMapper
import pt.isel.repositoryjdbi.mappers.ReportMapper
import pt.isel.repositoryjdbi.mappers.TokenMapper
import pt.isel.repositoryjdbi.mappers.UserMapper
import pt.isel.repositoryjdbi.mappers.VehicleMapper
import pt.isel.repositoryjdbi.mappers.WeatherConditionsMapper
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
            .registerRowMapper(AccidentCaseMapper())
            .registerRowMapper(WeatherConditionsMapper())
            .registerRowMapper(AccidentSceneMapper())
            .registerRowMapper(VehicleMapper())
            .registerRowMapper(DamageMapper())
            .registerRowMapper(EvidenceMapper())
            .registerRowMapper(ImageEvidenceMapper())
            .registerRowMapper(AnalysisMapper())
            .registerRowMapper(AnalysisImageMapper())
            .registerRowMapper(MeasurementMapper())
            .registerRowMapper(DamageComparisonMapper())
            .registerRowMapper(AnalysisConclusionMapper())
            .registerRowMapper(ReportMapper())

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
