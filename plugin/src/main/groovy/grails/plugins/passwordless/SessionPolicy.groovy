package grails.plugins.passwordless

import java.time.Duration

/**
 * How long a session may sit idle. The default is passwordless.session.idle for everyone; an application that wants
 * shorter sessions for administrators declares a bean named sessionPolicy.
 */
interface SessionPolicy {

    Duration idleTimeout(LoginPrincipal principal)
}
