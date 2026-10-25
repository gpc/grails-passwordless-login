package grails.plugins.passwordless

import java.time.Duration

/**
 * Delivers a login link: by mail, SMS or whatever the application uses. Implemented by the application, as a Spring
 * bean. Runs on a background thread when passwordless.async is on, so it has no request.
 */
interface LoginLinkSender {

    /**
     * @param context what the application passed with the request, such as the tenant login started from, or null
     */
    void send(LoginAccount account, URI link, Duration validFor, String context)
}
