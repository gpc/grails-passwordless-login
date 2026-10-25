package grails.plugins.passwordless

import grails.plugins.passwordless.passkey.PasskeyRegistrationAuthorization
import grails.plugins.passwordless.passkey.PasskeyRegistrationGuard
import grails.plugins.passwordless.passkey.PasskeySettings
import grails.plugins.passwordless.passkey.PasskeySignInHandler
import org.springframework.context.ApplicationContext
import org.springframework.http.HttpStatus
import org.springframework.security.config.ObjectPostProcessor
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.web.authentication.AuthenticationEntryPointFailureHandler
import org.springframework.security.web.authentication.HttpStatusEntryPoint
import org.springframework.security.web.authentication.LoginUrlAuthenticationEntryPoint
import org.springframework.security.web.csrf.CsrfFilter
import org.springframework.security.web.savedrequest.NullRequestCache
import org.springframework.security.web.webauthn.authentication.WebAuthnAuthenticationFilter

/**
 * The plugin's part of an application's SecurityFilterChain. The application owns the chain and calls this from it:
 *
 * <pre>
 * http.authorizeHttpRequests { auth ->
 *     PasswordlessSecurity.authorize(auth, applicationContext)
 *     auth.anyRequest().authenticated()
 * }
 * PasswordlessSecurity.configure(http, applicationContext)
 * </pre>
 *
 * Not statically compiled: the configurer DSL relies on closure-to-lambda coercion.
 */
class PasswordlessSecurity {

    /** What anyone may reach before signing in: the login pages, the passkey sign-in endpoints, the script. */
    static final List<String> PUBLIC_PATHS = ['/login', '/login/**', '/webauthn/authenticate/**', '/passwordless/**'].asImmutable()

    /**
     * Call before anyRequest(): permits {@link #PUBLIC_PATHS} and guards the passkey registration endpoints.
     */
    static void authorize(auth, ApplicationContext context) {
        auth.requestMatchers(*PUBLIC_PATHS).permitAll()
        auth.requestMatchers('/webauthn/register/**').access(context.getBean(PasskeyRegistrationAuthorization))
    }

    /**
     * Gives Spring's WebAuthn authentication filter the plugin's handlers for success and failure.
     */
    private static class SignInHandlers implements ObjectPostProcessor<WebAuthnAuthenticationFilter> {

        private final ApplicationContext context
        private final String afterPasskey

        SignInHandlers(ApplicationContext context, String afterPasskey) {
            this.context = context
            this.afterPasskey = afterPasskey
        }

        @Override
        <O extends WebAuthnAuthenticationFilter> O postProcess(O filter) {
            filter.authenticationSuccessHandler = new PasskeySignInHandler(
                    context.getBeanProvider(SignInService), context.getBeanProvider(LoginAccountLookup), afterPasskey)
            filter.authenticationFailureHandler = new AuthenticationEntryPointFailureHandler(
                    new HttpStatusEntryPoint(HttpStatus.UNAUTHORIZED))
            filter
        }
    }

    /**
     * Session, CSRF, passkeys, the login redirect and logout. Everything else about the chain stays the application's.
     */
    static HttpSecurity configure(HttpSecurity http, ApplicationContext context) {
        PasskeySettings settings = context.getBean(PasskeySettings)
        PasskeyRegistrationAuthorization registration = context.getBean(PasskeyRegistrationAuthorization)
        String afterPasskey = context.environment.getProperty('passwordless.login.successUrl', '/')
        http
                .addFilterAfter(new PasskeyRegistrationGuard(registration), CsrfFilter)
                .securityContext { it.securityContextRepository(context.getBean('securityContextRepository')) }
                .csrf { it.csrfTokenRepository(context.getBean('csrfTokenRepository')) }
                .webAuthn { webAuthn ->
                    webAuthn.rpName(settings.rpName)
                            .rpId(settings.rpId)
                            .allowedOrigins(settings.allowedOrigins)
                            .disableDefaultRegistrationPage(true)
                            .withObjectPostProcessor(new SignInHandlers(context, afterPasskey))
                }
                .exceptionHandling { it.authenticationEntryPoint(new LoginUrlAuthenticationEntryPoint('/login')) }
                .requestCache { it.requestCache(new NullRequestCache()) }
                .logout { it.logoutUrl('/logout').logoutSuccessUrl('/login?loggedOut') }
    }
}
