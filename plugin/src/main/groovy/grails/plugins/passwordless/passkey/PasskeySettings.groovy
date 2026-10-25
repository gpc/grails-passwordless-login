package grails.plugins.passwordless.passkey

import groovy.transform.CompileStatic
import org.springframework.core.env.Environment

/**
 * The WebAuthn relying party: by default the host and origin of grails.serverURL, the address users reach the
 * application at. passwordless.passkeys.rp-id and passwordless.passkeys.allowed-origins override it, which only a
 * test with a browser in a container needs.
 */
@CompileStatic
class PasskeySettings {

    final String rpId
    final String rpName
    final Set<String> allowedOrigins

    PasskeySettings(Environment environment) {
        URI server = URI.create(environment.getProperty('grails.serverURL', 'http://localhost:8080'))
        String origin = "${server.scheme}://${server.host}${server.port > 0 ? ":${server.port}" : ''}"
        rpId = environment.getProperty('passwordless.passkeys.rp-id') ?: server.host
        rpName = environment.getProperty('passwordless.passkeys.rp-name') ?: environment.getProperty('passwordless.app-name', 'Login')
        String origins = environment.getProperty('passwordless.passkeys.allowed-origins')
        allowedOrigins = (origins ? origins.split(',')*.trim().findAll() : [origin]) as Set<String>
    }
}
