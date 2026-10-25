package sample

import grails.plugins.passwordless.LoginAccount
import grails.plugins.passwordless.LoginLinkSender
import groovy.transform.CompileStatic
import groovy.transform.Immutable

import java.time.Duration
import java.util.concurrent.CopyOnWriteArrayList

/**
 * The application's side of the plugin: delivering a link. This one keeps them in memory, for specs to read.
 */
@CompileStatic
class LinkInbox implements LoginLinkSender {

    @Immutable
    static class Delivery {
        LoginAccount account
        URI link
        Duration validFor
        String context

        String getToken() {
            link.path.substring(link.path.lastIndexOf('/') + 1)
        }
    }

    private final List<Delivery> deliveries = new CopyOnWriteArrayList<>()

    @Override
    void send(LoginAccount account, URI link, Duration validFor, String context) {
        deliveries << new Delivery(account, link, validFor, context)
    }

    List<Delivery> to(String email) {
        deliveries.findAll { Delivery d -> d.account.email == email }
    }

    List<Delivery> getAll() {
        Collections.unmodifiableList(deliveries)
    }

    void clear() {
        deliveries.clear()
    }
}
