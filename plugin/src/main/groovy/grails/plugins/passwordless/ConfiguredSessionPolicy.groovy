package grails.plugins.passwordless

import groovy.transform.CompileStatic

import java.time.Duration

@CompileStatic
class ConfiguredSessionPolicy implements SessionPolicy {

    final Duration idle

    ConfiguredSessionPolicy(Duration idle) {
        this.idle = idle
    }

    @Override
    Duration idleTimeout(LoginPrincipal principal) {
        idle
    }
}
