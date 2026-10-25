package sample

import grails.plugins.passwordless.PasswordlessSecurity
import jakarta.servlet.DispatcherType
import org.springframework.context.ApplicationContext
import org.springframework.context.annotation.Bean
import org.springframework.context.annotation.Configuration
import org.springframework.security.config.annotation.web.builders.HttpSecurity
import org.springframework.security.config.annotation.web.configuration.EnableWebSecurity
import org.springframework.security.web.SecurityFilterChain

/**
 * What an application does to use the plugin: its own chain, with the plugin's parts before anyRequest() and after.
 * Not statically compiled: the configurer DSL relies on closure-to-lambda coercion.
 */
@Configuration
@EnableWebSecurity
class SecurityConfiguration {

    @Bean
    SecurityFilterChain securityFilterChain(HttpSecurity http, ApplicationContext context) {
        http.authorizeHttpRequests { auth ->
            auth.dispatcherTypeMatchers(DispatcherType.FORWARD, DispatcherType.ERROR, DispatcherType.INCLUDE).permitAll()
            PasswordlessSecurity.authorize(auth, context)
            auth.anyRequest().authenticated()
        }
        PasswordlessSecurity.configure(http, context)
        http.build()
    }
}
