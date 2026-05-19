package pt.isel

import com.fasterxml.jackson.databind.ObjectMapper
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
import org.springframework.http.MediaType
import org.springframework.test.context.TestPropertySource
import org.springframework.test.web.servlet.MockMvc
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.header
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath
import org.springframework.test.web.servlet.result.MockMvcResultMatchers.status
import pt.isel.domain.AccidentCase
import pt.isel.domain.User
import pt.isel.domain.UserRole
import pt.isel.http.CaseController
import pt.isel.http.UserController
import pt.isel.http.dto.CreateUserRequestDto
import pt.isel.http.dto.LoginRequestDto
import pt.isel.http.dto.UpdateCaseRequestDto
import pt.isel.services.CaseService
import pt.isel.services.TokenCreationError
import pt.isel.services.TokenError
import pt.isel.services.UserSession
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

    @Autowired
    private lateinit var objectMapper: ObjectMapper

    @MockBean
    private lateinit var userAuthService: UserAuthService

    @MockBean
    private lateinit var caseService: CaseService

    @Test
    fun `user can register`() {
        val request =
            CreateUserRequestDto(
                username = "new-user",
                email = "new-user@example.com",
                password = "StrongPass123",
            )
        val createdUser = user(userId = 3, username = request.username, email = request.email)
        given(userAuthService.createUser(request.username, request.email, request.password))
            .willReturn(success(createdUser))

        mockMvc.perform(
            post("/api/users")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)),
        )
            .andExpect(status().isCreated)
            .andExpect(header().string(HttpHeaders.LOCATION, "/api/users/${createdUser.userId}"))
            .andExpect(jsonPath("$.userId").value(createdUser.userId))
            .andExpect(jsonPath("$.username").value(createdUser.username))
            .andExpect(jsonPath("$.email").value(createdUser.email))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    @Test
    fun `user can login with valid credentials`() {
        val request = LoginRequestDto(email = regularUser.email, password = "StrongPass123")
        given(userAuthService.createToken(request.email, request.password))
            .willReturn(success(session(regularUser, USER_TOKEN)))

        mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)),
        )
            .andExpect(status().isOk)
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("idap_session=$USER_TOKEN")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Path=/")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=86400")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("HttpOnly")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("SameSite=Lax")))
            .andExpect(jsonPath("$.userId").value(regularUser.userId))
            .andExpect(jsonPath("$.username").value(regularUser.username))
            .andExpect(jsonPath("$.email").value(regularUser.email))
            .andExpect(jsonPath("$.role").doesNotExist())
    }

    @Test
    fun `login fails with wrong password`() {
        val request = LoginRequestDto(email = regularUser.email, password = "WrongPass123")
        given(userAuthService.createToken(request.email, request.password))
            .willReturn(failure(TokenCreationError.UserOrPasswordAreInvalid))

        mockMvc.perform(
            post("/api/users/login")
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(request)),
        )
            .andExpect(status().isUnauthorized)
            .andExpect(header().doesNotExist(HttpHeaders.SET_COOKIE))
            .andExpect(jsonPath("$.title").value("Invalid credentials"))
    }

    @Test
    fun `logout invalidates token and clears session cookie`() {
        given(userAuthService.getUserByToken(USER_TOKEN))
            .willReturn(success(regularUser), failure(TokenError.InvalidToken))
        given(userAuthService.deleteToken(USER_TOKEN)).willReturn(true)

        mockMvc.perform(post("/api/users/logout").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isNoContent)
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("idap_session=")))
            .andExpect(header().string(HttpHeaders.SET_COOKIE, containsString("Max-Age=0")))

        mockMvc.perform(get("/api/cases").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("Invalid token"))

        verify(userAuthService).deleteToken(USER_TOKEN)
    }

    @Test
    fun `anonymous cannot list users`() {
        mockMvc.perform(get("/api/users"))
            .andExpect(status().isUnauthorized)
            .andExpect(jsonPath("$.title").value("No user currently logged in"))
    }

    @Test
    fun `anonymous cannot get user by id`() {
        mockMvc.perform(get("/api/users/${regularUser.userId}"))
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
    fun `normal user cannot update another users case`() {
        givenValidSession(USER_TOKEN, regularUser)
        given(caseService.getCaseById(otherUserCase.caseId)).willReturn(success(otherUserCase))

        mockMvc.perform(
            put("/api/cases/${otherUserCase.caseId}")
                .cookie(sessionCookie(USER_TOKEN))
                .contentType(MediaType.APPLICATION_JSON)
                .content(json(UpdateCaseRequestDto(description = "Updated description", status = "closed"))),
        )
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.title").value("Case access denied"))

        verify(caseService, never()).updateCase(otherUserCase.caseId, "Updated description", "closed")
    }

    @Test
    fun `normal user cannot delete another users case`() {
        givenValidSession(USER_TOKEN, regularUser)
        given(caseService.getCaseById(otherUserCase.caseId)).willReturn(success(otherUserCase))

        mockMvc.perform(delete("/api/cases/${otherUserCase.caseId}").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.title").value("Case access denied"))

        verify(caseService, never()).deleteCase(otherUserCase.caseId)
    }

    @Test
    fun `normal user can access own profile`() {
        givenValidSession(USER_TOKEN, regularUser)
        given(userAuthService.getUserById(regularUser.userId)).willReturn(success(regularUser))

        mockMvc.perform(get("/api/users/${regularUser.userId}").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.userId").value(regularUser.userId))
            .andExpect(jsonPath("$.username").value(regularUser.username))
            .andExpect(jsonPath("$.email").value(regularUser.email))
            .andExpect(jsonPath("$.passwordHash").doesNotExist())
    }

    @Test
    fun `normal user cannot access another users profile`() {
        givenValidSession(USER_TOKEN, regularUser)

        mockMvc.perform(get("/api/users/${otherUser.userId}").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.title").value("Access denied"))

        verify(userAuthService, never()).getUserById(otherUser.userId)
    }

    @Test
    fun `normal user cannot delete users`() {
        givenValidSession(USER_TOKEN, regularUser)

        mockMvc.perform(delete("/api/users/${otherUser.userId}").cookie(sessionCookie(USER_TOKEN)))
            .andExpect(status().isForbidden)
            .andExpect(jsonPath("$.title").value("Access denied"))

        verify(userAuthService, never()).deleteUser(otherUser.userId)
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
    fun `admin can delete users`() {
        givenValidSession(ADMIN_TOKEN, adminUser)
        given(userAuthService.deleteUser(otherUser.userId)).willReturn(success(Unit))

        mockMvc.perform(delete("/api/users/${otherUser.userId}").cookie(sessionCookie(ADMIN_TOKEN)))
            .andExpect(status().isNoContent)

        verify(userAuthService).deleteUser(otherUser.userId)
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
    fun `admin can access any case`() {
        givenValidSession(ADMIN_TOKEN, adminUser)
        given(caseService.getCaseById(otherUserCase.caseId)).willReturn(success(otherUserCase))

        mockMvc.perform(get("/api/cases/${otherUserCase.caseId}").cookie(sessionCookie(ADMIN_TOKEN)))
            .andExpect(status().isOk)
            .andExpect(jsonPath("$.caseId").value(otherUserCase.caseId))
            .andExpect(jsonPath("$.userId").value(otherUser.userId))
            .andExpect(jsonPath("$.description").value(otherUserCase.accidentDescription))
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

    private fun givenValidSession(
        rawToken: String,
        user: User,
    ) {
        given(userAuthService.getUserByToken(rawToken)).willReturn(success(user))
    }

    private fun sessionCookie(rawToken: String) = Cookie("idap_session", rawToken)

    private fun json(value: Any): String = objectMapper.writeValueAsString(value)

    private companion object {
        const val USER_TOKEN = "user-token"
        const val ADMIN_TOKEN = "admin-token"
        const val INVALID_TOKEN = "invalid-token"
        const val EXPIRED_TOKEN = "expired-token"

        val regularUser =
            user(
                userId = 1,
                username = "regular",
                email = "regular@example.com",
            )

        val otherUser =
            user(
                userId = 2,
                username = "other",
                email = "other@example.com",
            )

        val adminUser =
            admin(
                userId = 99,
                username = "admin",
                email = "admin@example.com",
            )

        val regularUserCase =
            accidentCase(
                caseId = 10,
                owner = regularUser,
                createdAt = Instant.parse("2026-05-17T16:42:10Z"),
                description = "Regular user's case",
            )

        val otherUserCase =
            accidentCase(
                caseId = 20,
                owner = otherUser,
                createdAt = Instant.parse("2026-05-18T16:42:10Z"),
                description = "Other user's case",
            )

        fun user(
            userId: Int,
            username: String = "user$userId",
            email: String = "$username@example.com",
            role: UserRole = UserRole.USER,
        ) = User(
            userId = userId,
            username = username,
            email = email,
            passwordHash = "hash",
            role = role,
        )

        fun admin(
            userId: Int,
            username: String = "admin$userId",
            email: String = "$username@example.com",
        ) = user(
            userId = userId,
            username = username,
            email = email,
            role = UserRole.ADMIN,
        )

        fun session(
            user: User,
            rawToken: String,
        ) = UserSession(user, rawToken)

        fun accidentCase(
            caseId: Int,
            owner: User,
            createdAt: Instant,
            status: String = "open",
            description: String? = "${owner.username}'s case",
        ) = AccidentCase(
            caseId = caseId,
            userId = owner.userId,
            createdAt = createdAt,
            caseStatus = status,
            accidentDescription = description,
        )
    }
}
