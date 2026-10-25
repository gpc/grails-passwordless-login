package grails.plugins.passwordless.security

import groovy.transform.CompileStatic

import java.nio.charset.StandardCharsets
import java.security.MessageDigest
import java.security.SecureRandom

/**
 * Magic link tokens: 256 random bits, base64url, stored only as SHA-256. A fast hash is enough for a token with
 * this much entropy; a slow one would protect a guessable secret, which this is not.
 */
@CompileStatic
final class LoginTokens {

    private static final SecureRandom RANDOM = new SecureRandom()
    static final int TOKEN_BYTES = 32
    static final String FORMAT = /[A-Za-z0-9_-]{43}/

    private LoginTokens() {}

    static String generate() {
        byte[] bytes = new byte[TOKEN_BYTES]
        RANDOM.nextBytes(bytes)
        Base64.urlEncoder.withoutPadding().encodeToString(bytes)
    }

    static boolean isWellFormed(String token) {
        token != null && token ==~ FORMAT
    }

    static String hash(String token) {
        MessageDigest.getInstance('SHA-256').digest(token.getBytes(StandardCharsets.US_ASCII)).encodeHex().toString()
    }
}
