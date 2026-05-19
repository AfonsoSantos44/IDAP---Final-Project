package pt.isel

import jakarta.servlet.http.Cookie
import org.hamcrest.Matchers.containsString
import org.junit.jupiter.api.Test
import org.mockito.BDDMockito.given
import org.mockito.Mockito.never
import org.mockito.Mockito.verify
import org.springframework.beans.factory.annotation.Autowired
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest
import org.springframework.boot.test.mock.mockito.MockBean
import org.springframework.context.annotation.Import
import org.springframework.http.HttpHeaders
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pt.isel.domain.AccidentCase
import pt.isel.domain.User
import pt.isel.domain.UserRole
import pt.isel.http.CaseController
import pt.isel.http.UserController
import pt.isel.services.CaseService
import pt.isel.services.TokenError
import pt.isel.services.UserAuthService
import pt.isel.services.failure
import pt.isel.services.success
import java.time.Instant

@WebMvcTest(controllers = [UserController::class, CaseController::class])
@Import(SecurityConfig::class, SessionTokenAuthenticationFilter::class)
@TestPropertySource(properties = ["idap.session.cookie.secure=false"])
class SecurityAuthorizationTests {
    @Autowired
    private lateinit var mockMvc: MockMvc

    @MockBean
    private lateinit var userAuthService: UserAuthService

    @MockBean
    private lateinit var caseService: CaseService

    @Test
    fun `anonymous cannot list users`() {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("No user currently logged in"))
    }

    @Test
    fun `anonymous cannot delete users`() {
        mockMvc.perform(delete("/api/users/1"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("No user currently logged in"))
    }

    @Test
    fun `anonymous cannot list cases`() {
        mockMvc.perform(get("/api/cases"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("No user currently logged in"))
    }

    @Test
    fun `normal user only sees own cases`() {
        givenValidSession(USER_TOKEN, regularUser)
        given(caseService.getCasesByUserId(regularUser.userId)).willReturn(success(listOf(regularUserCase)))

        mockMvc.perform(get("/api/cases").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(1))
            .andExpect(jsonPath("$[0].userId").value(regularUser.userId))

        verify(caseService, never()).getCases()
    }

    @Test
    fun `normal user cannot access another users case`() {
        givenValidSession(USER_TOKEN, regularUser)
        given(caseService.getCaseById(otherUserCase.caseId)).willReturn(success(otherUserCase))

        mockMvc.perform(get("/api/cases/${otherUserCase.caseId}").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.title").value("Case access denied"))
    }

    @Test
    fun `normal user cannot access another users profile`() {
        givenValidSession(USER_TOKEN, regularUser)

        mockMvc.perform(get("/api/users/${otherUser.userId}").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.title").value("Access denied"))
    }

    @Test
    fun `admin can list users`() {
        givenValidSession(ADMIN_TOKEN, adminUser)
        given(userAuthService.getUsers()).willReturn(listOf(regularUser, otherUser, adminUser))

        mockMvc.perform(get("/api/users").cookie(sessionCookie(ADMIN_TOKEN)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(3))
    }

    @Test
    fun `admin can list all cases`() {
        givenValidSession(ADMIN_TOKEN, adminUser)
        given(caseService.getCases()).willReturn(listOf(regularUserCase, otherUserCase))

        mockMvc.perform(get("/api/cases").cookie(sessionCookie(ADMIN_TOKEN)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.length()").value(2))
    }

    @Test
    fun `invalid token is rejected`() {
        given(userAuthService.getUserByToken(INVALID_TOKEN)).willReturn(failure(TokenError.InvalidToken))

        mockMvc.perform(get("/api/cases").cookie(sessionCookie(INVALID_TOKEN)))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("Invalid token"))
    }

    @Test
    fun `expired token is rejected`() {
        given(userAuthService.getUserByToken(EXPIRED_TOKEN)).willReturn(failure(TokenError.ExpiredToken))

        mockMvc.perform(get("/api/cases").cookie(sessionCookie(EXPIRED_TOKEN)))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("Expired token"))
    }

    @Test
    fun `logout removes token and clears session cookie`() {
        givenValidSession(USER_TOKEN, regularUser)
        given(userAuthService.deleteToken(USER_TOKEN)).willReturn(true)

        mockMvc.perform(post("/api/users/logout").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isNoContent)
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("idap_session=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))

        verify(userAuthService).deleteToken(USER_TOKEN)
    }

    private fun givenValidSession(
        rawToken: String,
        user: User,
    ) {
        given(userAuthService.getUserByToken(rawToken)).willReturn(success(user))
    }

    private fun sessionCookie(rawToken: String) = Cookie("idap_session", rawToken)

    private companion object {
        const val USER_TOKEN = "user-token"
        const val ADMIN_TOKEN = "admin-token"
        const val INVALID_TOKEN = "invalid-token"
        const val EXPIRED_TOKEN = "expired-token"

        val regularUser =
            User(
                userId = 1,
                username = "regular",
                email = "regular@example.com",
                passwordHash = "hash",
                role = UserRole.USER,
            )

        val otherUser =
            User(
                userId = 2,
                username = "other",
                email = "other@example.com",
                passwordHash = "hash",
                role = UserRole.USER,
            )

        val adminUser =
            User(
                userId = 99,
                username = "admin",
                email = "admin@example.com",
                passwordHash = "hash",
                role = UserRole.ADMIN,
            )

        val regularUserCase =
            AccidentCase(
                caseId = 10,
                userId = regularUser.userId,
                createdAt = Instant.parse("2026-05-17T16:42:10Z"),
                caseStatus = "open",
                accidentDescription = "Regular user's case",
            )

        val otherUserCase =
            AccidentCase(
                caseId = 20,
                userId = otherUser.userId,
                createdAt = Instant.parse("2026-05-18T16:42:10Z"),
                caseStatus = "open",
                accidentDescription = "Other user's case",
            )
    }
}
