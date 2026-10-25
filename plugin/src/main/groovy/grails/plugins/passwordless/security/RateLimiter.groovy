package grails.plugins.passwordless.security

import groovy.transform.CompileStatic

import java.time.Clock
import java.time.Duration
import java.time.Instant
import java.util.concurrent.ConcurrentHashMap

/**
 * A sliding-window counter per key, in memory. One application instance and a restart that forgets the counts are
 * both acceptable for an endpoint whose worst case is a handful of extra messages.
 */
@CompileStatic
class RateLimiter {

    final int limit
    final Duration window
    private final Clock clock
    private final ConcurrentHashMap<String, ArrayDeque<Instant>> hits = new ConcurrentHashMap<>()

    RateLimiter(int limit, Duration window, Clock clock = Clock.systemUTC()) {
        this.limit = limit
        this.window = window
        this.clock = clock
    }

    /**
     * Records an attempt for the key and says whether it is within the limit. Attempts over the limit are not
     * recorded, so a blocked key recovers once its earlier attempts age out of the window.
     */
    boolean tryAcquire(String key) {
        Instant now = clock.instant()
        Instant cutoff = now - window
        boolean allowed = false
        hits.compute(key) { String k, ArrayDeque<Instant> times ->
            ArrayDeque<Instant> current = times ?: new ArrayDeque<Instant>()
            while (current && !current.peekFirst().isAfter(cutoff)) {
                current.pollFirst()
            }
            if (current.size() < limit) {
                current.addLast(now)
                allowed = true
            }
            current
        }
        if (hits.size() > 10_000) {
            prune(cutoff)
        }
        allowed
    }

    void reset() {
        hits.clear()
    }

    private void prune(Instant cutoff) {
        new ArrayList<String>(hits.keySet()).each { String key ->
            // computeIfPresent holds the entry's lock, and returning null removes it.
            hits.computeIfPresent(key) { String k, ArrayDeque<Instant> times ->
                times.isEmpty() || !times.peekLast().isAfter(cutoff) ? null : times
            }
        }
    }
}
