package grails.plugins.passwordless

/**
 * Where a user goes after signing in by magic link. The default is passwordless.login.successUrl. An application
 * that sends people to a tenant, say, declares a bean named loginSuccessRedirect.
 */
interface LoginSuccessRedirect {

    /** A path on this application, starting with a single slash. */
    String urlFor(LoginPrincipal principal, String context)
}
