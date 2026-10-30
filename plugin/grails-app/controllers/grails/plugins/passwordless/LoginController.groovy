package grails.plugins.passwordless

import grails.plugins.passwordless.passkey.PasskeySignInHandler
import grails.plugins.passwordless.security.LoginTokens

/**
 * Magic link sign-in. Opening a link only shows a confirmation page; the token is spent by the POST from that page,
 * so a mail scanner that prefetches links cannot use it up. With passwordless.login.confirmation: false the link
 * itself signs in, without the page.
 */
class LoginController {

    static namespace = null
    static allowedMethods = [form: 'GET', send: 'POST', sent: 'GET', confirm: 'GET', redeem: 'POST']

    MagicLinkService magicLinkService
    SignInService signInService
    LoginSuccessRedirect loginSuccessRedirect

    def form(String context) {
        [loggedOut: params.containsKey('loggedOut'), invalid: params.containsKey('invalid'), context: context]
    }

    def send(String email, String context) {
        if (!magicLinkService.allowRequestFrom(request.remoteAddr)) {
            response.status = 429
            render view: 'tooMany'
            return
        }
        magicLinkService.requestLink(email, request.remoteAddr, context)
        // The same answer whether or not the address is known.
        redirect controller: 'login', action: 'sent', method: 'GET', absolute: false
    }

    def sent() {
    }

    def confirm(String token) {
        response.setHeader('Cache-Control', 'no-store')
        if (!LoginTokens.isWellFormed(token)) {
            redirect controller: 'login', action: 'form', method: 'GET', params: [invalid: true], absolute: false
            return
        }
        if (!magicLinkService.confirmationRequired) {
            signInWith(token)
            return
        }
        [token: token]
    }

    def redeem(String token) {
        signInWith(token)
    }

    private void signInWith(String token) {
        RedeemedLogin login = magicLinkService.redeem(token)
        if (login == null) {
            redirect controller: 'login', action: 'form', method: 'GET', params: [invalid: true], absolute: false
            return
        }
        LoginPrincipal principal = signInService.signIn(login.account, request, response)
        sendLocalRedirect(loginSuccessRedirect.urlFor(principal, login.context))
    }

    /**
     * A relative redirect, which Grails' own redirect would make absolute from grails.serverURL, and so send a browser
     * that reached the application by another name to the wrong place. Only a path on this application.
     */
    private void sendLocalRedirect(String target) {
        response.sendRedirect(PasskeySignInHandler.isLocalPath(target) ? target : '/')
    }
}
