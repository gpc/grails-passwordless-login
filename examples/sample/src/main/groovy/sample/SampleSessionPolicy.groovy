package sample

import grails.plugins.passwordless.LoginPrincipal
import grails.plugins.passwordless.SessionPolicy
import groovy.transform.CompileStatic

import java.time.Duration

/**
 * An application that wants shorter sessions for some users declares a bean named sessionPolicy.
 */
@CompileStatic
class SampleSessionPolicy implements SessionPolicy {

    static final Duration ADMIN_IDLE = Duration.ofMinutes(30)
    static final Duration USER_IDLE = Duration.ofDays(14)

    @Override
    Duration idleTimeout(LoginPrincipal principal) {
        ((SamplePrincipal) principal).admin ? ADMIN_IDLE : USER_IDLE
    }
}
