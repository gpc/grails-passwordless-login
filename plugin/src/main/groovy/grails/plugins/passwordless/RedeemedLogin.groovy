package grails.plugins.passwordless

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * A spent magic link: whose it was, and the context the application attached when it was requested.
 */
@CompileStatic
@Immutable
class RedeemedLogin {

    LoginAccount account
    String context
}
