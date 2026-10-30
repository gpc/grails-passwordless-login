package grails.plugins.passwordless

import org.grails.config.PropertySourcesConfig
import spock.lang.Specification

import java.time.Duration

class MagicLinkServiceConfigSpec extends Specification {

    void "refuses to start without grails.serverURL: #description"() {
        when:
        new MagicLinkService().setConfiguration(new PropertySourcesConfig(settings))

        then:
        IllegalStateException e = thrown()
        e.message.contains('grails.serverURL')

        where:
        description | settings
        'missing'   | [:]
        'empty'     | ['grails.serverURL': '']
    }

    void "starts with grails.serverURL and reads the token validity"() {
        given:
        MagicLinkService service = new MagicLinkService()

        when:
        service.setConfiguration(new PropertySourcesConfig(['grails.serverURL': 'https://app.example', 'passwordless.login.tokenValidity': 'PT10M']))

        then:
        service.tokenValidity == Duration.ofMinutes(10)
    }

    void "defaults the token validity to fifteen minutes"() {
        given:
        MagicLinkService service = new MagicLinkService()

        when:
        service.setConfiguration(new PropertySourcesConfig(['grails.serverURL': 'https://app.example']))

        then:
        service.tokenValidity == Duration.ofMinutes(15)
    }

    void "confirmation is required unless switched off: #setting"() {
        given:
        MagicLinkService service = new MagicLinkService()
        Map settings = ['grails.serverURL': 'https://app.example']
        if (setting != null) {
            settings['passwordless.login.confirmation'] = setting
        }

        when:
        service.setConfiguration(new PropertySourcesConfig(settings))

        then:
        service.confirmationRequired == expected

        where:
        setting | expected
        null    | true
        true    | true
        false   | false
        'false' | false
    }
}
