package sample

import grails.plugins.passwordless.LoginToken
import grails.plugins.passwordless.MagicLinkService
import grails.plugins.passwordless.security.RateLimiter
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import java.net.http.HttpResponse

/**
 * Asking for links is limited per account and per address. The account limit is silent, so it does not reveal that an
 * address is known; the address limit is not, since it says nothing about any address.
 */
@Integration
class RateLimitSpec extends Specification {

    MagicLinkService magicLinkService
    LinkInbox linkInbox
    RateLimiter passwordlessRateLimiterPerAccount
    RateLimiter passwordlessRateLimiterPerAddress

    void setup() {
        linkInbox.clear()
        passwordlessRateLimiterPerAccount.reset()
        passwordlessRateLimiterPerAddress.reset()
    }

    void "an account gets at most five links an hour, and is not told when the limit is reached"() {
        given:
        Long userId = Seed.user('Flittige Frida')
        String email = Seed.emailOf(userId)

        when:
        7.times { magicLinkService.requestLink(email, "198.51.100.${it}") }

        then: 'five links and five rows, nothing for the sixth and seventh'
        linkInbox.to(email).size() == 5
        LoginToken.withNewTransaction { LoginToken.countByAccountId(userId.toString()) } == 5
    }

    void "the limit over HTTP answers as if nothing happened, whether the address is known or not"() {
        given:
        String known = Seed.emailOf(Seed.user('Kendte Knud'))
        List<String> locations = []

        when: 'one more request than the account is allowed'
        6.times { locations << new WebClient(serverPort).submit('/login', '/login', [email: known]).headers().firstValue('Location').get() }
        String unknown = new WebClient(serverPort).submit('/login', '/login', [email: "ukendt-${Seed.uid()}@example.org"]).headers().firstValue('Location').get()

        then: 'every answer is the same page'
        locations.every { it == '/login/sent' }
        unknown == '/login/sent'
        linkInbox.to(known).size() == 5
    }

    void "accounts are limited separately"() {
        given:
        String a = Seed.emailOf(Seed.user('Alma Alene'))
        String b = Seed.emailOf(Seed.user('Bent Alene'))

        when:
        6.times { magicLinkService.requestLink(a, '198.51.100.1') }
        magicLinkService.requestLink(b, '198.51.100.1')

        then:
        linkInbox.to(a).size() == 5
        linkInbox.to(b).size() == 1
    }

    void "an address may ask twenty times an hour, counted per address"() {
        expect:
        (1..20).every { magicLinkService.allowRequestFrom('192.0.2.1') }
        !magicLinkService.allowRequestFrom('192.0.2.1')
        magicLinkService.allowRequestFrom('192.0.2.2')
    }

    void "the twenty-first request from an address is refused with 429 and the too-many page"() {
        given:
        WebClient web = new WebClient(serverPort)
        String email = Seed.emailOf(Seed.user('Ivrige Ida'))

        when:
        List<Integer> statuses = (1..21).collect { web.submit('/login', '/login', [email: email]).statusCode() }
        HttpResponse<String> refused = web.submit('/login', '/login', [email: email])

        then: 'twenty answers, then 429'
        statuses.take(20).every { it == 302 }
        statuses.last() == 429
        refused.statusCode() == 429
        refused.body().contains('Too many login requests')
    }

    void "a request over the address limit sends nothing"() {
        given:
        String email = Seed.emailOf(Seed.user('Afvist Alf'))
        20.times { magicLinkService.allowRequestFrom('127.0.0.1') }

        when:
        HttpResponse<String> refused = new WebClient(serverPort).submit('/login', '/login', [email: email])

        then:
        refused.statusCode() == 429
        linkInbox.to(email).empty
    }

    void "the window slides: attempts count again once they age out"() {
        given:
        MutableClock clock = new MutableClock()
        RateLimiter limiter = new RateLimiter(2, java.time.Duration.ofHours(1), clock)

        when:
        boolean first = limiter.tryAcquire('k')
        clock.advance(java.time.Duration.ofMinutes(30))
        boolean second = limiter.tryAcquire('k')
        boolean third = limiter.tryAcquire('k')
        clock.advance(java.time.Duration.ofMinutes(31))
        boolean afterFirstAgedOut = limiter.tryAcquire('k')

        then:
        first && second
        !third
        afterFirstAgedOut
    }

    static class MutableClock extends java.time.Clock {

        java.time.Instant now = java.time.Instant.parse('2026-01-01T00:00:00Z')

        void advance(java.time.Duration by) {
            now = now + by
        }

        @Override
        java.time.ZoneId getZone() {
            java.time.ZoneOffset.UTC
        }

        @Override
        java.time.Clock withZone(java.time.ZoneId zone) {
            this
        }

        @Override
        java.time.Instant instant() {
            now
        }
    }
}
