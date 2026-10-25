package grails.plugins.passwordless

import grails.plugins.passwordless.passkey.PasskeySignInHandler
import org.springframework.security.core.Authentication
import org.springframework.security.core.context.SecurityContextHolder

/**
 * The signed-in user's own passkeys. Registering and signing in with one happen in the browser against Spring
 * Security's WebAuthn endpoints; this lists, removes, and asks the user to confirm one.
 */
class PasskeyController {

    static allowedMethods = [manage: 'GET', confirm: 'GET', delete: 'POST', script: 'GET']

    PasskeyService passkeyService
    LoginAccountLookup loginAccountLookup

    /**
     * The passkey script, from the plugin's jar. A controller rather than a static resource, so it is served whatever
     * the application does about static resources, and to someone who has not signed in.
     */
    def script() {
        byte[] js = PasskeyController.getResourceAsStream('/passwordless/passkeys.js').withCloseable { it.bytes }
        response.setHeader('Cache-Control', 'public, max-age=3600')
        render file: js, contentType: 'text/javascript;charset=UTF-8'
    }

    def manage() {
        LoginPrincipal principal = currentPrincipal()
        passkeyService.ensureUserEntity(loginAccountLookup.findByEmail(principal.email))
        [passkeys: passkeyService.passkeysOf(principal.email),
         verified: principal.passkeyVerified,
         mayRegister: principal.passkeyVerified || !passkeyService.hasPasskey(principal.email)]
    }

    /**
     * Where an application sends a user whose session has not confirmed a passkey: confirm one, or add the first.
     */
    def confirm() {
        LoginPrincipal principal = currentPrincipal()
        if (principal.passkeyVerified) {
            String target = grailsApplication.config.getProperty('passwordless.passkeys.confirmedUrl', String, '/')
            response.sendRedirect(PasskeySignInHandler.isLocalPath(target) ? target : '/')
            return
        }
        passkeyService.ensureUserEntity(loginAccountLookup.findByEmail(principal.email))
        [hasPasskey: passkeyService.hasPasskey(principal.email)]
    }

    def delete(String credentialId) {
        LoginPrincipal principal = currentPrincipal()
        if (!principal.passkeyVerified) {
            flash.error = message(code: 'passwordless.passkeys.confirmFirst')
        } else if (passkeyService.deleteOwn(principal.email, credentialId)) {
            flash.message = message(code: 'passwordless.passkeys.removed')
        } else {
            response.sendError(404)
            return
        }
        redirect controller: 'passkey', action: 'manage', method: 'GET', absolute: false
    }

    private static LoginPrincipal currentPrincipal() {
        Authentication authentication = SecurityContextHolder.context.authentication
        (LoginPrincipal) authentication.principal
    }
}
