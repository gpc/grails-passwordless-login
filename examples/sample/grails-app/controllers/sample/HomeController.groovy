package sample

import grails.plugins.passwordless.passkey.PasskeySignInHandler
import org.springframework.security.core.context.SecurityContextHolder

class HomeController {

    static allowedMethods = [index: 'GET', admin: 'GET']

    def index() {
        [principal: (SamplePrincipal) SecurityContextHolder.context.authentication.principal]
    }

    /**
     * A page that wants a passkey confirmed in the session: it sends the user to confirm one, and remembers where
     * they were going.
     */
    def admin() {
        SamplePrincipal principal = (SamplePrincipal) SecurityContextHolder.context.authentication.principal
        if (!principal.passkeyVerified) {
            session.setAttribute(PasskeySignInHandler.RETURN_TO, '/admin')
            redirect controller: 'passkey', action: 'confirm', method: 'GET', absolute: false
            return
        }
        render text: "Admin ${principal.displayName}", contentType: 'text/plain'
    }
}
