package grails.plugins.passwordless.security

import spock.lang.Specification

class LoginTokensSpec extends Specification {

    void "tokens are 43 base64url characters and unique"() {
        when:
        Set<String> tokens = (1..1000).collect { LoginTokens.generate() } as Set

        then:
        tokens.size() == 1000
        tokens.every { LoginTokens.isWellFormed(it) }
    }

    void "the hash is SHA-256 hex and never the token"() {
        given:
        String token = LoginTokens.generate()

        expect:
        LoginTokens.hash(token) ==~ /[0-9a-f]{64}/
        LoginTokens.hash(token) == LoginTokens.hash(token)
        !LoginTokens.hash(token).contains(token)
    }

    void "#value is rejected"() {
        expect:
        !LoginTokens.isWellFormed(value)

        where:
        value << [null, '', 'short', 'x' * 42, 'x' * 44, ('x' * 42) + '=', ('x' * 42) + '/', "${'x' * 40}' --".toString()]
    }
}
