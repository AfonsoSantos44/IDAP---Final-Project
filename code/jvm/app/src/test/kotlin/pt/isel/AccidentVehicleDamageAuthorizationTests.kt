package pt.isel

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.Cookie
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pt.isel.domain.AccidentCase
import pt.isel.domain.Damage
import pt.isel.domain.User
import pt.isel.domain.UserRole
import pt.isel.domain.Vehicle
import pt.isel.http.AccidentDataAccessControl
import pt.isel.http.AccidentVehicleDamageController
import pt.isel.http.dto.CreateDamageRequestDto
import pt.isel.services.AccidentAnalysisService
import pt.isel.services.AccidentConclusionReportService
import pt.isel.services.AccidentDamageComparisonService
import pt.isel.services.AccidentEvidenceService
import pt.isel.services.AccidentMeasurementService
import pt.isel.services.AccidentVehicleDamageService
import pt.isel.services.CaseService
import pt.isel.services.UserAuthService
import pt.isel.services.success
import java.time.Instant

@WebMvcTest(controllers = [AccidentVehicleDamageController::class])
@Import(SecurityConfig::class, SessionTokenAuthenticationFilter::class, AccidentDataAccessControl::class)
@TestPropertySource(properties = ["idap.session.cookie.secure=false"])
class AccidentVehicleDamageAuthorizationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userAuthService: UserAuthService

    @MockBean
    private lateinit var caseService: CaseService

    @MockBean
    private lateinit var vehicleDamageService: AccidentVehicleDamageService

    @MockBean
    private lateinit var evidenceService: AccidentEvidenceService

    @MockBean
    private lateinit var analysisService: AccidentAnalysisService

    @MockBean
    private lateinit var measurementService: AccidentMeasurementService

    @MockBean
    private lateinit var damageComparisonService: AccidentDamageComparisonService

    @MockBean
    private lateinit var conclusionReportService: AccidentConclusionReportService

    @Test
    fun `authenticated user can create damage for own vehicle`() {
        val request =
            CreateDamageRequestDto(
                contactZone = "front bumper",
                deformationType = "dent",
                direction = "front-to-back",
                damageDescription = "Visible deformation on front bumper",
            )
        val damage =
            Damage(
                damageId = 30,
                vehicleId = regularUserVehicle.vehicleId,
                contactZone = request.contactZone,
                deformationType = request.deformationType,
                direction = request.direction,
                heightCm = null,
                damageDescription = request.damageDescription,
            )

        given(userAuthService.getUserByToken(USER_TOKEN)).willReturn(success(regularUser))
        given(vehicleDamageService.getVehicleById(regularUserVehicle.vehicleId)).willReturn(success(regularUserVehicle))
        given(caseService.getCaseById(regularUserCase.caseId)).willReturn(success(regularUserCase))
        given(
            vehicleDamageService.createDamage(
                regularUserVehicle.vehicleId,
                request.contactZone,
                request.deformationType,
                request.direction,
                request.damageDescription,
            ),
        ).willReturn(success(damage))

        mockMvc.perform(
            post("/api/vehicles/${regularUserVehicle.vehicleId}/damages")
                .cookie(sessionCookie(USER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(header().string(HttpHeaders.LOCATION, "/api/damages/${damage.damageId}"))
            .andExpect(jsonPath("$.damageId").value(damage.damageId))
            .andExpect(jsonPath("$.vehicleId").value(damage.vehicleId))
            .andExpect(jsonPath("$.contactZone").value(damage.contactZone))
            .andExpect(jsonPath("$.damageDescription").value(damage.damageDescription))
    }

    private fun sessionCookie(rawToken: String) = Cookie("idap_session", rawToken)

    private companion object {
        const val USER_TOKEN = "user-token"

        val regularUser =
            User(
                userId = 1,
                username = "regular",
                email = "regular@example.com",
                passwordHash = "hash",
                role = UserRole.USER,
            )

        val regularUserCase =
            AccidentCase(
                caseId = 10,
                userId = regularUser.userId,
                createdAt = Instant.parse("2026-05-17T16:42:10Z"),
                caseStatus = "open",
                accidentDescription = "Regular user's case",
            )

        val regularUserVehicle =
            Vehicle(
                vehicleId = 1,
                caseId = regularUserCase.caseId,
                brand = "Toyota",
                model = "Yaris",
                yearOfFabrication = 2020,
                licensePlate = "AA-00-AA",
                role = "vehicle_a",
            )
    }
}
