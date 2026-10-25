package grails.plugins.passwordless

import groovy.transform.CompileStatic
import org.springframework.security.core.userdetails.User
import org.springframework.security.core.userdetails.UserDetails
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.core.userdetails.UsernameNotFoundException

/**
 * Looks an account up by email for Spring Security's WebAuthn provider, which needs a UserDetailsService once a
 * passkey checks out. Nobody signs in with a password: no chain accepts one, and the password here is a random
 * value nobody knows. Declaring it also keeps Spring Boot from creating its default in-memory user.
 */
@CompileStatic
class LookupUserDetailsService implements UserDetailsService {

    private final LoginAccountLookup lookup

    LookupUserDetailsService(LoginAccountLookup lookup) {
        this.lookup = lookup
    }

    @Override
    UserDetails loadUserByUsername(String email) throws UsernameNotFoundException {
        if (lookup.findByEmail(email) == null) {
            throw new UsernameNotFoundException('No such account')
        }
        User.withUsername(email).password('{noop}' + UUID.randomUUID()).authorities('ROLE_USER').build()
    }
}
