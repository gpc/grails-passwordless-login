package sample

import grails.plugins.passwordless.LoginPrincipal
import grails.plugins.passwordless.LoginToken
import grails.plugins.passwordless.MagicLinkService
import grails.plugins.passwordless.RedeemedLogin
import grails.plugins.passwordless.SignInService
import grails.plugins.passwordless.security.LoginTokens
import grails.plugins.passwordless.security.RateLimiter
import grails.testing.mixin.integration.Integration
import groovy.sql.Sql
import org.springframework.mock.web.MockHttpServletRequest
import org.springframework.mock.web.MockHttpServletResponse
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import spock.lang.Specification

import javax.sql.DataSource
import java.net.http.HttpResponse
import java.time.Duration
import java.time.Instant

/**
 * Magic links: what is stored, who gets one, that a link works once and expires, and the session it ends in.
 */
@Integration
class MagicLinkSpec extends Specification {

    MagicLinkService magicLinkService
    SignInService signInService
    LinkInbox linkInbox
    RateLimiter passwordlessRateLimiterPerAccount
    RateLimiter passwordlessRateLimiterPerAddress
    DataSource dataSource

    void setup() {
        linkInbox.clear()
        passwordlessRateLimiterPerAccount.reset()
        passwordlessRateLimiterPerAddress.reset()
    }

    void cleanup() {
        SecurityContextHolder.clearContext()
    }

    void "a known address gets one link, and only the token's hash is stored"() {
        given:
        Long userId = Seed.user('Anne Andersen')
        String email = Seed.emailOf(userId)

        when: 'the address is typed with different case and spacing'
        magicLinkService.requestLink("  ${email.toUpperCase()} ", '203.0.113.5')

        then:
        linkInbox.to(email).size() == 1
        LinkInbox.Delivery delivery = linkInbox.to(email).first()
        delivery.account.id == userId.toString()
        delivery.validFor == Duration.ofMinutes(15)

        and: 'the link is built from grails.serverURL and the mapped path, with a token of the right shape'
        delivery.link.toString() == "http://localhost/login/token/${delivery.token}"
        LoginTokens.isWellFormed(delivery.token)

        and:
        List<LoginToken> rows = tokensOf(userId)
        rows.size() == 1
        rows.first().tokenHash == LoginTokens.hash(delivery.token)
        rows.first().requestedFrom == '203.0.113.5'
        rows.first().usedAt == null
        Duration.between(rows.first().createdAt, rows.first().expiresAt) == Duration.ofMinutes(15)

        and: 'the token is in no column'
        new Sql(dataSource).rows('select * from login_token where account_id = ?', [userId.toString()])
                .every { row -> !row.values().any { String.valueOf(it).contains(delivery.token) } }
    }

    void "an unknown or disabled address gets no row and no message"() {
        given:
        String disabled = Seed.emailOf(Seed.user('Deaktiveret Dorte', false))
        long tokensBefore = count('login_token')

        when:
        magicLinkService.requestLink("nobody-${Seed.uid()}@example.org", '203.0.113.5')
        magicLinkService.requestLink(disabled, '203.0.113.5')

        then:
        linkInbox.all.empty
        count('login_token') == tokensBefore
    }

    void "a link works exactly once"() {
        given:
        Long userId = Seed.user('Bo Brugsen')
        String token = requestToken(userId)

        when:
        RedeemedLogin first = magicLinkService.redeem(token)

        then:
        first.account.id == userId.toString()
        magicLinkService.redeem(token) == null
        tokensOf(userId).first().usedAt != null
    }

    void "two simultaneous redemptions cannot both succeed"() {
        given:
        String token = requestToken(Seed.user('Samtidige Sam'))

        when:
        List<RedeemedLogin> outcomes = Collections.synchronizedList([])
        List<Thread> threads = (1..8).collect { Thread.start { outcomes << magicLinkService.redeem(token) } }
        threads*.join()

        then:
        outcomes.count { it != null } == 1
        outcomes.count { it == null } == 7
    }

    void "an expired link is refused, and stays unspent"() {
        given:
        Long userId = Seed.user('Edith Expired')
        String token = LoginTokens.generate()
        insertToken(userId, token, Instant.now() - Duration.ofMinutes(20), Instant.now() - Duration.ofMinutes(5))

        expect:
        magicLinkService.redeem(token) == null
        tokensOf(userId).first().usedAt == null
    }

    void "a link for an account that has been disabled since is refused"() {
        given:
        Long userId = Seed.user('Senere Deaktiveret')
        String token = requestToken(userId)
        Seed.disable(userId)

        expect:
        magicLinkService.redeem(token) == null
    }

    void "a malformed or unknown token is refused: #token"() {
        expect:
        magicLinkService.redeem(token) == null

        where:
        token << [null, '', 'not-a-token', LoginTokens.generate()]
    }

    void "opening the link spends nothing, only confirming it does, so a mail scanner cannot use it up"() {
        given:
        Long userId = Seed.user('Scannet Sanne')
        String token = requestToken(userId)
        WebClient web = new WebClient(serverPort)

        when: 'a scanner opens the link'
        HttpResponse<String> opened = web.get("/login/token/${token}")

        then: 'it sees a confirmation page and the token is still unspent'
        opened.statusCode() == 200
        opened.headers().firstValue('Cache-Control').get().contains('no-store')
        tokensOf(userId).first().usedAt == null
        web.get('/').statusCode() == 302

        when: 'the person confirms'
        HttpResponse<String> confirmed = web.submit("/login/token/${token}", '/login/token', [token: token])

        then: 'they are signed in, and sent to the success url'
        confirmed.statusCode() == 302
        URI.create(confirmed.headers().firstValue('Location').get()).path == '/'
        web.get('/').body().contains('Hello Scannet Sanne')
        tokensOf(userId).first().usedAt != null

        when: 'the same link is tried again'
        HttpResponse<String> again = new WebClient(serverPort).submit("/login/token/${token}", '/login/token', [token: token])

        then:
        again.statusCode() == 302
        again.headers().firstValue('Location').get() == '/login?invalid=true'
    }

    void "a malformed token in the address goes back to the login page"() {
        expect:
        new WebClient(serverPort).get('/login/token/not-a-token').headers().firstValue('Location').get() == '/login?invalid=true'
    }

    void "what the application attached to the request comes back when the link is redeemed"() {
        given:
        Long userId = Seed.user('Kontekst Kim')
        String email = Seed.emailOf(userId)

        when:
        magicLinkService.requestLink(email, '203.0.113.5', 'glrye')

        then:
        linkInbox.to(email).first().context == 'glrye'
        magicLinkService.redeem(linkInbox.to(email).first().token).context == 'glrye'
        tokensOf(userId).first().context == 'glrye'
    }

    void "a context is cut to its column, not refused"() {
        given:
        String email = Seed.emailOf(Seed.user('Lang Lise'))

        when:
        magicLinkService.requestLink(email, '203.0.113.5', 'x' * 300)

        then:
        linkInbox.to(email).first().context.size() == MagicLinkService.MAX_CONTEXT_LENGTH
    }

    void "signing in changes the session id and sets the idle timeout the session policy gives: admin #admin"() {
        given:
        Long userId = Seed.user('Session Sanne', true, admin)
        String email = Seed.emailOf(userId)
        MockHttpServletRequest request = new MockHttpServletRequest()
        String before = request.getSession(true).id
        MockHttpServletResponse response = new MockHttpServletResponse()

        when:
        LoginPrincipal principal = signInService.signIn(
                new grails.plugins.passwordless.LoginAccount(userId.toString(), email, 'Session Sanne'), request, response)

        then:
        request.session.id != before
        request.session.maxInactiveInterval == idle.seconds
        ((SamplePrincipal) request.session.getAttribute(HttpSessionSecurityContextRepository.SPRING_SECURITY_CONTEXT_KEY)
                .authentication.principal).userId == userId
        !principal.passkeyVerified
        principal.authorities*.authority == [admin ? 'ROLE_ADMIN' : 'ROLE_USER']

        where:
        admin | idle
        true  | SampleSessionPolicy.ADMIN_IDLE
        false | SampleSessionPolicy.USER_IDLE
    }

    void "a token that expired more than a day ago is deleted when the next one is issued"() {
        given:
        Long old = Seed.user('Gammel Gerda')
        insertToken(old, LoginTokens.generate(), Instant.now() - Duration.ofDays(3), Instant.now() - Duration.ofDays(3) + Duration.ofMinutes(15))
        Long recent = Seed.user('Nylig Nanna')
        insertToken(recent, LoginTokens.generate(), Instant.now() - Duration.ofHours(2), Instant.now() - Duration.ofHours(2) + Duration.ofMinutes(15))

        when:
        magicLinkService.requestLink(Seed.emailOf(Seed.user('Ny Nina')), '203.0.113.5')

        then: 'the old one is gone, and a spent one from this morning is kept'
        tokensOf(old).empty
        tokensOf(recent).size() == 1
    }

    void "an application that removes an account removes its tokens"() {
        given:
        Long userId = Seed.user('Fjernet Finn')
        requestToken(userId)
        passwordlessRateLimiterPerAccount.reset()
        requestToken(userId)

        when:
        int removed = magicLinkService.purgeTokensOf(userId.toString())

        then:
        removed == 2
        tokensOf(userId).empty
    }

    // -- helpers -----------------------------------------------------------------------------------------------

    private String requestToken(Long userId) {
        magicLinkService.requestLink(Seed.emailOf(userId), '203.0.113.5')
        linkInbox.to(Seed.emailOf(userId)).last().token
    }

    private void insertToken(Long userId, String token, Instant createdAt, Instant expiresAt) {
        LoginToken.withNewTransaction {
            new LoginToken(accountId: userId.toString(), tokenHash: LoginTokens.hash(token), createdAt: createdAt, expiresAt: expiresAt)
                    .save(failOnError: true, flush: true)
        }
    }

    private static List<LoginToken> tokensOf(Long userId) {
        LoginToken.withNewTransaction { LoginToken.findAllByAccountId(userId.toString()) }
    }

    private long count(String table) {
        new Sql(dataSource).firstRow("select count(*) as n from ${table}".toString()).n as long
    }
}
