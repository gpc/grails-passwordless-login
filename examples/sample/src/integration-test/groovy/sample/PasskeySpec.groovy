package sample

import grails.plugins.passwordless.PasskeyService
import grails.plugins.passwordless.passkey.PasskeyRegisteredEvent
import grails.plugins.passwordless.security.RateLimiter
import grails.testing.mixin.integration.Integration
import spock.lang.Specification

import java.net.http.HttpResponse

/**
 * Passkeys over HTTP with a software authenticator: registering, signing in with one, who may add or remove one, and
 * the session a passkey sign-in ends in.
 */
@Integration
class PasskeySpec extends Specification {

    LinkInbox linkInbox
    PasskeyLog passkeyLog
    PasskeyService passkeyService
    RateLimiter passwordlessRateLimiterPerAccount
    RateLimiter passwordlessRateLimiterPerAddress

    void setup() {
        linkInbox.clear()
        passkeyLog.clear()
        passwordlessRateLimiterPerAccount.reset()
        passwordlessRateLimiterPerAddress.reset()
    }

    void "a first passkey is added from a session signed in by link, announced, and signs in without a link"() {
        given:
        Long userId = Seed.user('Peter Passkey')
        String email = Seed.emailOf(userId)
        WebClient web = signedInByLink(userId)

        when:
        HttpResponse<String> registered = Passkeys.register(web, email, 'Min telefon')

        then:
        registered.statusCode() == 200
        registered.body().contains('"success":true')

        and: 'the application is told, to tell the user'
        passkeyLog.events.size() == 1
        passkeyLog.events.first().label == 'Min telefon'
        passkeyService.emailOf(passkeyLog.events.first().userId) == email

        and: 'the passkey is listed'
        passkeyService.passkeysOf(email)*.label == ['Min telefon']
        web.get('/account/passkeys').body().contains('Min telefon')

        when: 'a session with no link signs in with the passkey alone'
        WebClient fresh = new WebClient(serverPort)
        HttpResponse<String> signedIn = Passkeys.signIn(fresh, email)

        then:
        signedIn.statusCode() == 200
        signedIn.body().contains('"authenticated":true')
        signedIn.body().contains('"redirectUrl":"/"')
        fresh.get('/').body().contains('Hello Peter Passkey')
        linkInbox.to(email).size() == 1
    }

    void "signing in again with a passkey is not announced as a new passkey"() {
        given:
        Long userId = Seed.user('Gentagne Gitte')
        String email = Seed.emailOf(userId)
        Passkeys.register(signedInByLink(userId), email)
        passkeyLog.clear()

        when:
        2.times { Passkeys.signIn(new WebClient(serverPort), email) }

        then:
        passkeyLog.events.empty
    }

    void "a second passkey needs a session that has confirmed the first"() {
        given:
        Long userId = Seed.user('Anden Anna')
        String email = Seed.emailOf(userId)
        Passkeys.register(signedInByLink(userId), email, 'Første')

        when: 'a session signed in by link only tries to add one'
        HttpResponse<String> byLink = Passkeys.register(signedInByLink(userId), email, 'Ukendt')

        then:
        byLink.statusCode() == 403
        passkeyService.passkeysOf(email)*.label == ['Første']

        when: 'a session that signed in with the passkey adds one'
        WebClient confirmed = new WebClient(serverPort)
        Passkeys.signIn(confirmed, email)
        HttpResponse<String> byPasskey = Passkeys.register(confirmed, email, 'Anden')

        then:
        byPasskey.statusCode() == 200
        passkeyService.passkeysOf(email)*.label == ['Første', 'Anden']
    }

    void "nobody who is not signed in can ask to register a passkey"() {
        given:
        WebClient anonymous = new WebClient(serverPort)
        String csrf = WebClient.passkeyCsrfToken(anonymous.get('/login').body())

        expect:
        anonymous.postJson('/webauthn/register/options', null, csrf).statusCode() in [401, 403, 302]
        anonymous.postJson('/webauthn/register', '{}', csrf).statusCode() in [401, 403, 302]
    }

    void "removing a passkey needs a confirmed session, and only one's own can be removed"() {
        given:
        Long userId = Seed.user('Fjerne Frede')
        String email = Seed.emailOf(userId)
        Passkeys.register(signedInByLink(userId), email, 'Gammel telefon')
        String credentialId = passkeyService.passkeysOf(email).first().credentialId.toBase64UrlString()
        Long otherId = Seed.user('Anden Alma')
        String otherEmail = Seed.emailOf(otherId)
        Passkeys.register(signedInByLink(otherId), otherEmail, 'Almas telefon')

        when: 'a session signed in by link tries'
        WebClient byLink = signedInByLink(userId)
        HttpResponse<String> refused = deletePasskey(byLink, credentialId)

        then: 'nothing is removed, and the page says why'
        refused.statusCode() == 302
        passkeyService.passkeysOf(email).size() == 1
        byLink.get('/account/passkeys').body().contains('Log in with one of your passkeys')

        when: 'a confirmed session tries to remove somebody else\'s'
        WebClient confirmed = new WebClient(serverPort)
        Passkeys.signIn(confirmed, email)
        String othersCredential = passkeyService.passkeysOf(otherEmail).first().credentialId.toBase64UrlString()
        HttpResponse<String> notTheirs = deletePasskey(confirmed, othersCredential)

        then:
        notTheirs.statusCode() == 404
        passkeyService.passkeysOf(otherEmail).size() == 1

        when: 'it removes its own'
        HttpResponse<String> removed = deletePasskey(confirmed, credentialId)

        then:
        removed.statusCode() == 302
        passkeyService.passkeysOf(email).empty
    }

    void "a page that wants a passkey sends the user to confirm one, and back to the page after"() {
        given:
        Long userId = Seed.user('Admin Alfred', true, true)
        String email = Seed.emailOf(userId)
        WebClient web = signedInByLink(userId)

        when: 'the page is asked for without a confirmed passkey'
        HttpResponse<String> asked = web.get('/admin')

        then:
        asked.statusCode() == 302
        asked.headers().firstValue('Location').get() == '/account/passkeys/confirm'
        web.get('/account/passkeys/confirm').body().contains('Create and confirm')

        when: 'a first passkey is created and confirmed'
        Passkeys.register(web, email)
        HttpResponse<String> confirmed = Passkeys.signIn(web, email)

        then: 'the sign-in answers with the page that was asked for'
        confirmed.body().contains('"redirectUrl":"/admin"')
        web.get('/admin').body() == 'Admin Admin Alfred'

        and: 'the confirm page now has nothing to ask, and sends the user on'
        URI.create(web.get('/account/passkeys/confirm').headers().firstValue('Location').get()).path == '/'
    }

    void "the confirm page asks an account that has a passkey to use it"() {
        given:
        Long userId = Seed.user('Har Hanne')
        String email = Seed.emailOf(userId)
        Passkeys.register(signedInByLink(userId), email)

        expect:
        signedInByLink(userId).get('/account/passkeys/confirm').body().contains('Confirm with a passkey')
    }

    void "an account that has been disabled can no longer sign in with its passkey"() {
        given:
        Long userId = Seed.user('Senere Stoppet')
        String email = Seed.emailOf(userId)
        Passkeys.register(signedInByLink(userId), email)
        Seed.disable(userId)

        when:
        HttpResponse<String> signedIn = Passkeys.signIn(new WebClient(serverPort), email)

        then:
        signedIn.statusCode() == 401
    }

    void "removing every passkey of an account is for the application to do, and leaves nothing behind"() {
        given:
        Long userId = Seed.user('Ryd Rasmus')
        String email = Seed.emailOf(userId)
        Passkeys.register(signedInByLink(userId), email, 'Første')

        expect:
        passkeyService.deleteAll(email) == 1
        passkeyService.passkeysOf(email).empty
        !passkeyService.hasPasskey(email)
    }

    void "the script is served to someone who has not signed in"() {
        when:
        HttpResponse<String> script = new WebClient(serverPort).get('/passwordless/passkeys.js')

        then:
        script.statusCode() == 200
        script.headers().firstValue('Content-Type').get().startsWith('text/javascript')
        script.body().contains('/webauthn/authenticate/options')
    }

    void "the login and passkey pages carry the CSRF token the script needs, with nothing from the layout"() {
        given:
        WebClient web = signedInByLink(Seed.user('Token Tine'))

        expect:
        new WebClient(serverPort).get('/login').body() =~ /data-csrf-header="[^"]+" data-csrf="[^"]+"/
        web.get('/account/passkeys').body() =~ /data-csrf-header="[^"]+" data-csrf="[^"]+"/
    }

    // -- helpers -----------------------------------------------------------------------------------------------

    /**
     * Posts the removal as the page's button would, with a token from any page of the session: a session that has not
     * confirmed a passkey is not shown the button, but can still send the request.
     */
    private HttpResponse<String> deletePasskey(WebClient web, String credentialId) {
        web.post('/account/passkeys/delete', [credentialId: credentialId, _csrf: WebClient.passkeyCsrfToken(web.get('/login').body())])
    }

    private WebClient signedInByLink(Long userId) {
        String email = Seed.emailOf(userId)
        WebClient web = new WebClient(serverPort)
        HttpResponse<String> asked = web.submit('/login', '/login', [email: email])
        assert asked.statusCode() == 302
        String token = linkInbox.to(email).last().token
        HttpResponse<String> confirmed = web.submit("/login/token/${token}", '/login/token', [token: token])
        assert confirmed.statusCode() == 302
        web
    }
}
