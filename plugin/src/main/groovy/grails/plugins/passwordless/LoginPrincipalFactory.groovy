package grails.plugins.passwordless

/**
 * Builds the session principal for an account that has just proved who they are. Implemented by the application, as
 * a Spring bean.
 */
interface LoginPrincipalFactory {

    LoginPrincipal principalFor(LoginAccount account, boolean passkeyVerified)
}
