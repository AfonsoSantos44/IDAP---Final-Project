package pt.isel

import com.fasterxml.jackson.databind.ObjectMapper
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.http.HttpMethod
import org.springframework.http.HttpStatus
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.http.SessionCreationPolicy
import org.springframework.security.web.SecurityFilterChain
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter
import pt.isel.http.Uris
import pt.isel.http.dto.Problem
import pt.isel.services.TokenError

@Configuration
class SecurityConfig(
    private val objectMapper: ObjectMapper,
) {

    @Bean
    fun securityFilterChain(
        http: HttpSecurity,
        sessionTokenAuthenticationFilter: SessionTokenAuthenticationFilter,
    ): SecurityFilterChain =
        http
            .csrf { it.disable() }
            .sessionManagement { it.sessionCreationPolicy(SessionCreationPolicy.STATELESS) }
            .formLogin { it.disable() }
            .httpBasic { it.disable() }
            .logout { it.disable() }
            .addFilterBefore(sessionTokenAuthenticationFilter, UsernamePasswordAuthenticationFilter::class.java)
            .exceptionHandling {
                it.authenticationEntryPoint { request, response, _ ->
                    writeProblemResponse(response, HttpStatus.UNAUTHORIZED, authenticationProblem(request))
                }
                it.accessDeniedHandler { _, response, _ ->
                    writeProblemResponse(response, HttpStatus.FORBIDDEN, Problem.AccessDenied)
                }
            }
            .authorizeHttpRequests {
                it.requestMatchers(HttpMethod.OPTIONS, "/**").permitAll()
                    .requestMatchers(HttpMethod.POST, Uris.Users.CREATE).permitAll()
                    .requestMatchers(HttpMethod.POST, Uris.Users.LOGIN).permitAll()
                    .requestMatchers("/docs/**", "/v3/api-docs/**", "/swagger-ui/**", "/swagger-ui.html").permitAll()
                    .requestMatchers(HttpMethod.GET, Uris.Users.ME).authenticated()
                    .requestMatchers(HttpMethod.POST, Uris.Users.LOGOUT).authenticated()
                    .requestMatchers(HttpMethod.GET, Uris.Users.LIST).hasRole("ADMIN")
                    .requestMatchers(HttpMethod.DELETE, "/api/users/*").hasRole("ADMIN")
                    .requestMatchers(HttpMethod.GET, "/api/users/*/cases").authenticated()
                    .requestMatchers(HttpMethod.GET, "/api/users/*").authenticated()
                    .requestMatchers(Uris.Cases.LIST, "/api/cases/**").authenticated()
                    .anyRequest().authenticated()
            }
            .build()

    private fun authenticationProblem(request: HttpServletRequest): Problem =
        when (request.getAttribute(SessionTokenAuthenticationFilter.TOKEN_ERROR_ATTRIBUTE)) {
            TokenError.InvalidToken -> Problem.InvalidToken
            TokenError.ExpiredToken -> Problem.ExpiredToken
            else -> Problem.NoUserLoggedIn
        }

    private fun writeProblemResponse(
        response: HttpServletResponse,
        status: HttpStatus,
        problem: Problem,
    ) {
        if (response.isCommitted) return

        response.status = status.value()
        response.contentType = "application/problem+json"
        objectMapper.writeValue(response.outputStream, problem)
    }
}