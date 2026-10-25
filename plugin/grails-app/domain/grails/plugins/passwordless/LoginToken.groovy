package grails.plugins.passwordless

import grails.compiler.GrailsCompileStatic

import java.time.Instant

/**
 * A magic link, stored only as the SHA-256 of its token. Single use, short lived.
 *
 * Not tenant-owned: it is redeemed before there is a session. It names the account by the application's own id.
 */
@GrailsCompileStatic
class LoginToken {

    String accountId
    String tokenHash
    Instant createdAt
    Instant expiresAt
    Instant usedAt
    String requestedFrom
    /** What the application attached to the request, handed back when the link is redeemed. */
    String context

    static constraints = {
        accountId nullable: false, blank: false, maxSize: 64
        tokenHash nullable: false, blank: false, size: 64..64, unique: true
        createdAt nullable: false
        expiresAt nullable: false
        usedAt nullable: true
        requestedFrom nullable: true, maxSize: 45
        context nullable: true, maxSize: 100
    }

    static mapping = {
        table 'login_token'
        version false
        accountId index: 'login_token_account_idx'
    }

    @Override
    String toString() {
        "LoginToken(${id}, account ${accountId}, expires ${expiresAt}, used ${usedAt})"
    }
}
