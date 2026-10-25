package grails.plugins.passwordless.security

import grails.plugins.passwordless.LoginPrincipal
import groovy.transform.CompileStatic
import org.springframework.security.authentication.AbstractAuthenticationToken

/**
 * A session established by redeeming a magic link or by a passkey. The authorities mirror the snapshot in the
 * principal; an application that needs current rights checks them against its own data.
 */
@CompileStatic
class LoginAuthentication extends AbstractAuthenticationToken {

    private final LoginPrincipal principal

    LoginAuthentication(LoginPrincipal principal) {
        super(principal.authorities)
        this.principal = principal
        super.setAuthenticated(true)
    }

    @Override
    Object getCredentials() {
        null
    }

    @Override
    LoginPrincipal getPrincipal() {
        principal
    }

    /**
     * The email, not a display name: Spring Security's WebAuthn support finds a person's passkeys by this name.
     */
    @Override
    String getName() {
        principal.email
    }

    @Override
    void setAuthenticated(boolean authenticated) {
        if (authenticated) {
            throw new IllegalArgumentException('Construct a new LoginAuthentication instead')
        }
        super.setAuthenticated(false)
    }
}
