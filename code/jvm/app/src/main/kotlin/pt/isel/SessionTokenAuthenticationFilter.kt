package pt.isel

import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.stereotype.Component
import org.springframework.web.filter.OncePerRequestFilter
import pt.isel.domain.toSecurityPrincipal
import pt.isel.services.Failure
import pt.isel.services.Success
import pt.isel.services.UserAuthService

@Component
class SessionTokenAuthenticationFilter(
    private val userAuthService: UserAuthService,
) : OncePerRequestFilter() {
    companion object {
        const val SESSION_COOKIE_NAME = "idap_session"
        const val TOKEN_ERROR_ATTRIBUTE = "pt.isel.security.tokenError"
    }

    override fun doFilterInternal(
        request: HttpServletRequest,
        response: HttpServletResponse,
        filterChain: FilterChain,
    ) {
        SecurityContextHolder.clearContext()

        val rawToken = request.cookies?.firstOrNull { it.name == SESSION_COOKIE_NAME }?.value
        if (!rawToken.isNullOrBlank()) {
            when (val result = userAuthService.getUserByToken(rawToken)) {
                is Success -> {
                    val principal = result.value.toSecurityPrincipal()
                    val authentication =
                        UsernamePasswordAuthenticationToken(
                            principal,
                            null,
                            listOf(SimpleGrantedAuthority("ROLE_${principal.role.name}")),
                        )
                    SecurityContextHolder.getContext().authentication = authentication
                }

                is Failure -> request.setAttribute(TOKEN_ERROR_ATTRIBUTE, result.value)
            }
        }

        try {
            filterChain.doFilter(request, response)
        } finally {
            SecurityContextHolder.clearContext()
        }
    }
}