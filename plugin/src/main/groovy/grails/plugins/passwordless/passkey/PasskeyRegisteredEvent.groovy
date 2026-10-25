package grails.plugins.passwordless.passkey

import groovy.transform.CompileStatic
import org.springframework.security.web.webauthn.api.Bytes

/**
 * A passkey was added to a user's account. Published by {@link NotifyingUserCredentialRepository}; an application
 * listens to tell the user, and whoever else should know.
 */
@CompileStatic
class PasskeyRegisteredEvent {

    final Bytes userId
    final String label

    PasskeyRegisteredEvent(Bytes userId, String label) {
        this.userId = userId
        this.label = label
    }
}
