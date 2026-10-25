package grails.plugins.passwordless

import grails.config.Config
import grails.core.support.GrailsConfigurationAware
import grails.gorm.transactions.NotTransactional
import grails.gorm.transactions.Transactional
import grails.plugins.passwordless.security.LoginTokens
import grails.plugins.passwordless.security.RateLimiter
import grails.web.mapping.LinkGenerator
import groovy.util.logging.Slf4j
import org.springframework.core.task.TaskExecutor

import java.time.Duration
import java.time.Instant

/**
 * Issues and redeems magic links. This is session recovery, not signup: it never creates an account, and answers
 * identically whether or not the address is known.
 */
@Slf4j
class MagicLinkService implements GrailsConfigurationAware {

    static final Duration TOKEN_RETENTION = Duration.ofDays(1)
    static final int MAX_CONTEXT_LENGTH = 100

    LoginAccountLookup loginAccountLookup
    LoginLinkSender loginLinkSender
    RateLimiter passwordlessRateLimiterPerAccount
    RateLimiter passwordlessRateLimiterPerAddress
    TaskExecutor passwordlessExecutor
    LinkGenerator grailsLinkGenerator

    Duration tokenValidity

    @Override
    void setConfiguration(Config config) {
        tokenValidity = Duration.parse(config.getProperty('passwordless.login.tokenValidity', String, 'PT15M'))
        // Without it the link generator falls back to the request's Host header, which the requester controls, and
        // links are built on a background thread that has no request at all.
        if (!config.getProperty('grails.serverURL', String)) {
            throw new IllegalStateException('grails.serverURL must be set: login links are built from it')
        }
    }

    /**
     * The per-address limit. Safe to answer visibly, since it says nothing about whether an address is known.
     */
    @NotTransactional
    boolean allowRequestFrom(String remoteAddress) {
        passwordlessRateLimiterPerAddress.tryAcquire("address:${remoteAddress}")
    }

    /**
     * Hands the request to a background thread and returns at once, so the response time does not reveal whether
     * the address belongs to anyone.
     */
    @NotTransactional
    void requestLink(String email, String remoteAddress, String context = null) {
        passwordlessExecutor.execute {
            try {
                issueLink(email, remoteAddress, context)
            } catch (Exception e) {
                log.error('Could not issue a login link', e)
            }
        }
    }

    /**
     * Creates a token and delivers the link, if the address belongs to an account under its limit. Otherwise does
     * nothing: no row, no message.
     */
    @Transactional
    void issueLink(String email, String remoteAddress, String context = null) {
        String normalised = email?.trim()?.toLowerCase(Locale.ROOT)
        LoginAccount account = normalised ? loginAccountLookup.findByEmail(normalised) : null
        if (account == null) {
            return
        }
        // Silent, unlike the per-address limit: answering differently would reveal that the address is known.
        if (!passwordlessRateLimiterPerAccount.tryAcquire("account:${account.id}")) {
            log.warn('Login link rate limit reached for account {}', account.id)
            return
        }
        // Stored and handed to the sender in the same form
        String boundedContext = context?.take(MAX_CONTEXT_LENGTH)
        Instant now = Instant.now()
        LoginToken.executeUpdate('delete from LoginToken t where t.expiresAt < :cutoff', [cutoff: now - TOKEN_RETENTION])
        String token = LoginTokens.generate()
        new LoginToken(accountId: account.id, tokenHash: LoginTokens.hash(token), createdAt: now,
                expiresAt: now + tokenValidity, requestedFrom: remoteAddress,
                context: boundedContext).save(failOnError: true, flush: true)
        URI link = URI.create(grailsLinkGenerator.link(controller: 'login', action: 'confirm', params: [token: token], method: 'GET', absolute: true))
        loginLinkSender.send(account, link, tokenValidity, boundedContext)
        log.info('Login link issued to account {}', account.id)
    }

    /**
     * Claims the token and returns who it was for, or null if it is unknown, expired or already used. The claim is
     * a single conditional update, so two simultaneous redemptions cannot both succeed.
     */
    @Transactional
    RedeemedLogin redeem(String token) {
        if (!LoginTokens.isWellFormed(token)) {
            return null
        }
        String hash = LoginTokens.hash(token)
        Instant now = Instant.now()
        int claimed = LoginToken.executeUpdate(
                'update LoginToken t set t.usedAt = :now where t.tokenHash = :hash and t.usedAt is null and t.expiresAt > :now',
                [now: now, hash: hash])
        if (claimed != 1) {
            return null
        }
        LoginToken spent = LoginToken.findByTokenHash(hash)
        LoginAccount account = loginAccountLookup.findById(spent.accountId)
        account == null ? null : new RedeemedLogin(account, spent.context)
    }

    /**
     * Deletes every token of an account, for an application that removes the account.
     */
    @Transactional
    int purgeTokensOf(String accountId) {
        LoginToken.executeUpdate('delete from LoginToken t where t.accountId = :accountId', [accountId: accountId])
    }
}
