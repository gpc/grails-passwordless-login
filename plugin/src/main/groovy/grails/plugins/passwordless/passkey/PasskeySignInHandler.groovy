package grails.plugins.passwordless.passkey

import grails.plugins.passwordless.LoginAccount
import grails.plugins.passwordless.LoginAccountLookup
import grails.plugins.passwordless.SignInService
import groovy.json.JsonOutput
import groovy.transform.CompileStatic
import jakarta.servlet.http.HttpServletRequest
import jakarta.servlet.http.HttpServletResponse
import jakarta.servlet.http.HttpSession
import org.springframework.beans.factory.ObjectProvider
import org.springframework.security.core.Authentication
import org.springframework.security.web.authentication.AuthenticationSuccessHandler

/**
 * Ends a passkey sign-in exactly as a magic link ends: the session gets the application's principal, a new id, a
 * fresh CSRF token and the same idle timeout. The principal is marked passkey-verified.
 *
 * Answers in the JSON Spring Security's WebAuthn client contract expects: {"authenticated": true, "redirectUrl": ...}.
 */
@CompileStatic
class PasskeySignInHandler implements AuthenticationSuccessHandler {

    /**
     * Where the user was going when an application sent them to confirm a passkey. An application sets it in the
     * session; only a path on this application is honoured.
     */
    static final String RETURN_TO = PasskeySignInHandler.name + '.returnTo'

    private final ObjectProvider<SignInService> signInService
    private final ObjectProvider<LoginAccountLookup> lookup
    private final String defaultUrl

    PasskeySignInHandler(ObjectProvider<SignInService> signInService, ObjectProvider<LoginAccountLookup> lookup, String defaultUrl = '/') {
        this.signInService = signInService
        this.lookup = lookup
        this.defaultUrl = defaultUrl
    }

    @Override
    void onAuthenticationSuccess(HttpServletRequest request, HttpServletResponse response, Authentication authentication) {
        LoginAccount account = lookup.getObject().findByEmail(authentication.name)
        if (account == null) {
            response.sendError(HttpServletResponse.SC_UNAUTHORIZED)
            return
        }
        String returnTo = returnTo(request.getSession(false))
        signInService.getObject().signIn(account, request, response, true)
        response.contentType = 'application/json'
        response.writer.write(JsonOutput.toJson([authenticated: true, redirectUrl: returnTo ?: defaultUrl]))
    }

    static boolean isLocalPath(String target) {
        target != null && target.startsWith('/') && !target.startsWith('//') && !target.contains('\\') && !target.contains('://')
    }

    private static String returnTo(HttpSession session) {
        String target = (String) session?.getAttribute(RETURN_TO)
        session?.removeAttribute(RETURN_TO)
        isLocalPath(target) ? target : null
    }
}
