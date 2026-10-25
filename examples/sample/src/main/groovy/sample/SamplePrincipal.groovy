package sample

import grails.plugins.passwordless.LoginPrincipal
import groovy.transform.CompileStatic
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority

@CompileStatic
final class SamplePrincipal implements LoginPrincipal {

    final Long userId
    final String email
    final String displayName
    final boolean admin
    final boolean passkeyVerified

    SamplePrincipal(Long userId, String email, String displayName, boolean admin, boolean passkeyVerified) {
        this.userId = userId
        this.email = email
        this.displayName = displayName
        this.admin = admin
        this.passkeyVerified = passkeyVerified
    }

    @Override
    Collection<? extends GrantedAuthority> getAuthorities() {
        [new SimpleGrantedAuthority(admin ? 'ROLE_ADMIN' : 'ROLE_USER')]
    }
}
