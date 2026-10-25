package grails.plugins.passwordless.security

import spock.lang.Specification

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.time.ZoneOffset

class RateLimiterSpec extends Specification {

    static class MutableClock extends Clock {
        Instant now = Instant.parse('2026-10-17T12:00:00Z')
        ZoneOffset zone = ZoneOffset.UTC
        Clock withZone(java.time.ZoneId zone) { this }
        java.time.ZoneId getZone() { zone }
        Instant instant() { now }
    }

    MutableClock clock = new MutableClock()
    RateLimiter limiter = new RateLimiter(3, Duration.ofHours(1), clock)

    void "allows up to the limit within the window, then refuses"() {
        expect:
        (1..3).every { limiter.tryAcquire('a') }
        !limiter.tryAcquire('a')
    }

    void "keys are counted separately"() {
        given:
        3.times { limiter.tryAcquire('a') }

        expect:
        limiter.tryAcquire('b')
    }

    void "attempts age out of the window"() {
        given:
        3.times { limiter.tryAcquire('a') }

        when:
        clock.now = clock.now + Duration.ofMinutes(61)

        then:
        limiter.tryAcquire('a')
    }

    void "refused attempts are not counted, so a blocked key recovers"() {
        given:
        3.times { limiter.tryAcquire('a') }
        10.times { limiter.tryAcquire('a') }

        when:
        clock.now = clock.now + Duration.ofMinutes(61)

        then:
        (1..3).every { limiter.tryAcquire('a') }
    }

    void "the window slides rather than resetting on the hour"() {
        given:
        limiter.tryAcquire('a')
        clock.now = clock.now + Duration.ofMinutes(40)
        2.times { limiter.tryAcquire('a') }

        when:
        clock.now = clock.now + Duration.ofMinutes(21)

        then: 'only the first attempt has aged out'
        limiter.tryAcquire('a')
        !limiter.tryAcquire('a')
    }
}
