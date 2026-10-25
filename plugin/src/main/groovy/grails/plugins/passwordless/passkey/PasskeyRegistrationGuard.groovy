package grails.plugins.passwordless.passkey

import groovy.transform.CompileStatic
import jakarta.servlet.FilterChain
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.http.HttpMethod
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.servlet.util.matcher.PathPatternRequestMatcher
import org.springframework.security.web.util.matcher.OrRequestMatcher
import org.springframework.security.web.util.matcher.RequestMatcher
import org.springframework.web.filter.OncePerRequestFilter

/**
 * Applies {@link PasskeyRegistrationAuthorization} in front of Spring Security's registration filters. They answer
 * before the authorization filter runs, so without this a session that may not register would still be handed
 * registration options.
 */
@CompileStatic
class PasskeyRegistrationGuard extends OncePerRequestFilter {

    private final PasskeyRegistrationAuthorization authorization
    private final RequestMatcher registration = new OrRequestMatcher(
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, '/webauthn/register/options'),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.POST, '/webauthn/register'),
            PathPatternRequestMatcher.withDefaults().matcher(HttpMethod.DELETE, '/webauthn/register/**'))

    PasskeyRegistrationGuard(PasskeyRegistrationAuthorization authorization) {
        this.authorization = authorization
    }

    @Override
    protected void doFilterInternal(HttpServletRequest request, HttpServletResponse response, FilterChain chain) {
        if (registration.matches(request) && !authorization.allows(SecurityContextHolder.context.authentication)) {
            response.sendError(HttpServletResponse.SC_FORBIDDEN)
            return
        }
        chain.doFilter(request, response)
    }
}
