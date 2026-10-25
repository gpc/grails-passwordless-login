package grails.plugins.passwordless

/**
 * The plugin's URLs. POST /logout, /webauthn/* and /login/webauthn are Spring Security's and never reach a
 * controller.
 */
class PasswordlessUrlMappings {

    static mappings = {
        '/login'(controller: 'login', action: 'form', method: 'GET')
        '/login'(controller: 'login', action: 'send', method: 'POST')
        '/login/sent'(controller: 'login', action: 'sent', method: 'GET')
        "/login/token/$token"(controller: 'login', action: 'confirm', method: 'GET')
        '/login/token'(controller: 'login', action: 'redeem', method: 'POST')

        '/passwordless/passkeys.js'(controller: 'passkey', action: 'script', method: 'GET')

        '/account/passkeys'(controller: 'passkey', action: 'manage', method: 'GET')
        '/account/passkeys/confirm'(controller: 'passkey', action: 'confirm', method: 'GET')
        '/account/passkeys/delete'(controller: 'passkey', action: 'delete', method: 'POST')
    }
}
