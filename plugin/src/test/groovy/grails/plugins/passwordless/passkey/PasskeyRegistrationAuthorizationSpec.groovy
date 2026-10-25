package grails.plugins.passwordless.passkey

import grails.plugins.passwordless.LoginPrincipal
import grails.plugins.passwordless.security.LoginAuthentication
import org.springframework.security.authentication.TestingAuthenticationToken
import org.springframework.security.core.GrantedAuthority
import org.springframework.security.core.authority.SimpleGrantedAuthority
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository
import spock.lang.Specification

class PasskeyRegistrationAuthorizationSpec extends Specification {

    static class Principal implements LoginPrincipal {
        String email = 'ada@example.org'
        boolean passkeyVerified
        Collection<? extends GrantedAuthority> authorities = [new SimpleGrantedAuthority('ROLE_USER')]
    }

    PublicKeyCredentialUserEntityRepository entities = Mock()
    UserCredentialRepository credentials = Mock()
    PasskeyRegistrationAuthorization authorization = new PasskeyRegistrationAuthorization(entities, credentials)

    void "who may register a passkey: #description"() {
        given:
        def entity = ImmutablePublicKeyCredentialUserEntity.builder().name('ada@example.org').id(Bytes.random()).displayName('Ada').build()
        entities.findByUsername('ada@example.org') >> (hasPasskey ? entity : null)
        credentials.findByUserId(_) >> (hasPasskey ? [Mock(CredentialRecord)] : [])

        expect:
        authorization.allows(new LoginAuthentication(new Principal(passkeyVerified: verified))) == allowed

        where:
        description                       | hasPasskey | verified | allowed
        'first passkey, magic link'       | false      | false    | true
        'has a passkey, only magic link'  | true       | false    | false
        'has a passkey, confirmed one'    | true       | true     | true
    }

    void "nobody who is not signed in may register one"() {
        expect:
        !authorization.allows(null)
        !authorization.allows(new TestingAuthenticationToken('x', 'y'))
    }
}
