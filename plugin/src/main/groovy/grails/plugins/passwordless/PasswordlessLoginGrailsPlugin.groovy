package grails.plugins.passwordless

import grails.plugins.Plugin
import grails.plugins.passwordless.passkey.NotifyingUserCredentialRepository
import grails.plugins.passwordless.passkey.PasskeyRegistrationAuthorization
import grails.plugins.passwordless.passkey.PasskeySettings
import grails.plugins.passwordless.security.RateLimiter
import grails.util.Environment
import org.springframework.core.task.SyncTaskExecutor
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.scheduling.concurrent.ThreadPoolTaskExecutor
import org.springframework.security.core.userdetails.UserDetailsService
import org.springframework.security.web.context.HttpSessionSecurityContextRepository
import org.springframework.security.web.csrf.HttpSessionCsrfTokenRepository
import org.springframework.security.web.webauthn.management.JdbcPublicKeyCredentialUserEntityRepository

import java.time.Duration

class PasswordlessLoginGrailsPlugin extends Plugin {

    def grailsVersion = '8.0.0 > *'

    def title = 'Passwordless Login'
    def description = 'Magic link and passkey sign-in on plain Spring Security 7. The application supplies the users, ' +
            'the link delivery and the session principal.'
    def license = 'APACHE'
    def developers = [[name: 'Søren Berg Glasius', email: 'soeren@glasius.dk']]
    def documentation = 'https://gpc.github.io/grails-passwordless-login'
    def issueManagement = [system: 'GITHUB', url: 'https://github.com/gpc/grails-passwordless-login/issues']
    def scm = [url: 'https://github.com/gpc/grails-passwordless-login']
    def profiles = ['web']

    /**
     * An application overrides any of these by declaring a bean of the same name in resources.groovy. It must
     * supply loginAccountLookup, loginLinkSender and loginPrincipalFactory.
     */
    Closure doWithSpring() {
        { ->
            def config = grailsApplication.config
            securityContextRepository(HttpSessionSecurityContextRepository)
            csrfTokenRepository(HttpSessionCsrfTokenRepository)

            passkeySettings(PasskeySettings, ref('environment'))
            passwordlessJdbcTemplate(JdbcTemplate, ref('dataSource'))
            publicKeyCredentialUserEntityRepository(JdbcPublicKeyCredentialUserEntityRepository, ref('passwordlessJdbcTemplate'))
            // The only UserCredentialRepository bean: with two, Spring Security's WebAuthn configurer ignores both
            userCredentialRepository(NotifyingUserCredentialRepository, ref('passwordlessJdbcTemplate'))
            passkeyRegistrationAuthorization(PasskeyRegistrationAuthorization,
                    ref('publicKeyCredentialUserEntityRepository'), ref('userCredentialRepository'))

            // WebAuthn needs a UserDetailsService once a passkey checks out. Nobody signs in with a password: no
            // chain accepts one, and the password below is a random value nobody knows. Declaring this also keeps
            // Spring Boot from creating its default in-memory user.
            userDetailsService(LookupUserDetailsService, ref('loginAccountLookup'))

            sessionPolicy(ConfiguredSessionPolicy, Duration.parse(config.getProperty('passwordless.session.idle', String, 'P14D')))
            loginSuccessRedirect(ConfiguredLoginSuccessRedirect, config.getProperty('passwordless.login.successUrl', String, '/'))

            Duration window = Duration.parse(config.getProperty('passwordless.login.rateLimit.window', String, 'PT1H'))
            passwordlessRateLimiterPerAccount(RateLimiter, config.getProperty('passwordless.login.rateLimit.perAccount', Integer, 5), window)
            passwordlessRateLimiterPerAddress(RateLimiter, config.getProperty('passwordless.login.rateLimit.perAddress', Integer, 20), window)

            // Messages leave on a background thread, so a link request answers in the same time whether or not the
            // address is known.
            if (!config.getProperty('passwordless.async', Boolean, true) || Environment.current == Environment.TEST) {
                passwordlessExecutor(SyncTaskExecutor)
            } else {
                passwordlessExecutor(ThreadPoolTaskExecutor) {
                    corePoolSize = 1
                    maxPoolSize = 2
                    queueCapacity = 200
                    threadNamePrefix = 'passwordless-'
                }
            }
        }
    }
}
