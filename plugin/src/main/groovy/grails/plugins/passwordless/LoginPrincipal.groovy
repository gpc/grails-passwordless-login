package grails.plugins.passwordless

import org.springframework.security.core.GrantedAuthority

/**
 * What the session holds after sign-in. The application decides what else it carries: roles, memberships, the tenant
 * it is bound to. Kept in the HTTP session, so serializable and free of domain objects.
 */
interface LoginPrincipal extends Serializable {

    /** The sign-in name, the same address as {@link LoginAccount#getEmail()}. */
    String getEmail()

    /** Whether a passkey was used in this session, to sign in or to confirm. */
    boolean isPasskeyVerified()

    Collection<? extends GrantedAuthority> getAuthorities()
}
