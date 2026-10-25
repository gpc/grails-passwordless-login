package grails.plugins.passwordless

/**
 * How the plugin finds a user. Implemented by the application, as a Spring bean. Called outside any transaction
 * the plugin opens, so an implementation uses its own.
 */
interface LoginAccountLookup {

    /** @param email already trimmed and lowercased */
    LoginAccount findByEmail(String email)

    LoginAccount findById(String id)
}
