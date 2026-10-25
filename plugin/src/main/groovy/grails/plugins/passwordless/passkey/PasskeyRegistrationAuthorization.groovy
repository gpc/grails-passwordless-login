package grails.plugins.passwordless.passkey

import grails.plugins.passwordless.LoginPrincipal
import groovy.transform.CompileStatic
import org.springframework.security.authorization.AuthorizationDecision
import org.springframework.security.authorization.AuthorizationManager
import org.springframework.security.authorization.AuthorizationResult
import org.springframework.security.core.Authentication
import org.springframework.security.web.access.intercept.RequestAuthorizationContext
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository

import java.util.function.Supplier

/**
 * Who may add or remove a passkey: a signed-in person whose session has confirmed a passkey, or anyone adding their
 * first. Otherwise a session from a hijacked mailbox could add a passkey of its own to an account that has one.
 */
@CompileStatic
class PasskeyRegistrationAuthorization implements AuthorizationManager<RequestAuthorizationContext> {

    private final PublicKeyCredentialUserEntityRepository userEntities
    private final UserCredentialRepository credentials

    PasskeyRegistrationAuthorization(PublicKeyCredentialUserEntityRepository userEntities, UserCredentialRepository credentials) {
        this.userEntities = userEntities
        this.credentials = credentials
    }

    @Override
    AuthorizationResult authorize(Supplier<? extends Authentication> authentication, RequestAuthorizationContext context) {
        new AuthorizationDecision(allows(authentication.get()))
    }

    boolean allows(Authentication current) {
        Object principal = current?.authenticated ? current.principal : null
        if (!(principal instanceof LoginPrincipal)) {
            return false
        }
        LoginPrincipal person = (LoginPrincipal) principal
        person.passkeyVerified || !hasPasskey(person.email)
    }

    private boolean hasPasskey(String email) {
        PublicKeyCredentialUserEntity entity = email ? userEntities.findByUsername(email) : null
        entity != null && !credentials.findByUserId(entity.id).isEmpty()
    }
}
