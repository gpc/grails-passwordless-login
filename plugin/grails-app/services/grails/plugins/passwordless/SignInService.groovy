package grails.plugins.passwordless

import grails.plugins.passwordless.security.LoginAuthentication
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import org.springframework.security.core.context.SecurityContext
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.SecurityContextRepository
import org.springframework.security.web.csrf.CsrfTokenRepository

/**
 * Turns a redeemed magic link or a verified passkey into a session.
 */
class SignInService {

    SecurityContextRepository securityContextRepository
    CsrfTokenRepository csrfTokenRepository
    LoginPrincipalFactory loginPrincipalFactory
    SessionPolicy sessionPolicy

    /**
     * Establishes the session: a new session id against fixation, a fresh CSRF token, the idle timeout the session
     * policy gives this principal, and the application's principal as the authentication.
     */
    LoginPrincipal signIn(LoginAccount account, HttpServletRequest request, HttpServletResponse response, boolean passkeyVerified = false) {
        LoginPrincipal principal = loginPrincipalFactory.principalFor(account, passkeyVerified)
        if (request.getSession(false) != null) {
            request.changeSessionId()
        }
        request.getSession(true).maxInactiveInterval = (int) sessionPolicy.idleTimeout(principal).seconds
        csrfTokenRepository.saveToken(null, request, response)
        bind(principal, request, response)
        principal
    }

    /**
     * Replaces the principal of the current session without a new session id, for an application that rebinds a
     * session, to another tenant say, or marks it passkey-confirmed.
     */
    void bind(LoginPrincipal principal, HttpServletRequest request, HttpServletResponse response) {
        SecurityContext context = SecurityContextHolder.createEmptyContext()
        context.authentication = new LoginAuthentication(principal)
        SecurityContextHolder.context = context
        securityContextRepository.saveContext(context, request, response)
    }
}
