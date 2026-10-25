package grails.plugins.passwordless

import groovy.transform.CompileStatic
import groovy.transform.Immutable

/**
 * Who can sign in, as far as the plugin needs to know: a stable id, the address login links go to and passkeys are
 * registered under, and a name to show in authenticators. Whatever else an application knows about its users stays
 * with the application.
 */
@CompileStatic
@Immutable
class LoginAccount implements Serializable {

    /** The application's own id for the user. Stored as a string, so any id type works. */
    String id
    /** Lowercase. The sign-in name: Spring Security's WebAuthn support finds a user's passkeys by it. */
    String email
    String displayName
}
