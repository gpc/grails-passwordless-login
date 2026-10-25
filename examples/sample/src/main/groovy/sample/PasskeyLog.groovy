package sample

import grails.plugins.passwordless.passkey.PasskeyRegisteredEvent
import groovy.transform.CompileStatic
import org.springframework.context.event.EventListener

import java.util.concurrent.CopyOnWriteArrayList

/**
 * An application listens to PasskeyRegisteredEvent to tell the user, and whoever else should know.
 */
@CompileStatic
class PasskeyLog {

    final List<PasskeyRegisteredEvent> events = new CopyOnWriteArrayList<>()

    @EventListener
    void onRegistered(PasskeyRegisteredEvent event) {
        events << event
    }

    void clear() {
        events.clear()
    }
}
