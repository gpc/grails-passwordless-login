package grails.plugins.passwordless.passkey

import org.springframework.mock.env.MockEnvironment
import spock.lang.Specification

class PasskeySettingsSpec extends Specification {

    void "takes the relying party from grails.serverURL"() {
        when:
        PasskeySettings settings = new PasskeySettings(new MockEnvironment().withProperty('grails.serverURL', serverUrl))

        then:
        settings.rpId == rpId
        settings.allowedOrigins == [origin] as Set

        where:
        serverUrl                  | rpId          | origin
        'https://byposten.dk'      | 'byposten.dk' | 'https://byposten.dk'
        'http://localhost:8080'    | 'localhost'   | 'http://localhost:8080'
        'https://app.example:8443' | 'app.example' | 'https://app.example:8443'
    }

    void "explicit settings win"() {
        when:
        PasskeySettings settings = new PasskeySettings(new MockEnvironment()
                .withProperty('grails.serverURL', 'https://byposten.dk')
                .withProperty('passwordless.passkeys.rp-id', 'example.org')
                .withProperty('passwordless.passkeys.allowed-origins', 'https://a.example.org, https://b.example.org')
                .withProperty('passwordless.passkeys.rp-name', 'Byposten'))

        then:
        settings.rpId == 'example.org'
        settings.rpName == 'Byposten'
        settings.allowedOrigins == ['https://a.example.org', 'https://b.example.org'] as Set
    }
}
