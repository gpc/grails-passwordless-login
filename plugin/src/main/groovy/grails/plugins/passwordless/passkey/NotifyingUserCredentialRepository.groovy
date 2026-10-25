package grails.plugins.passwordless.passkey

import groovy.transform.CompileStatic
import org.springframework.context.ApplicationEventPublisher
import org.springframework.context.ApplicationEventPublisherAware
import org.springframework.jdbc.core.JdbcTemplate
import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.management.JdbcUserCredentialRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository

/**
 * Spring Security's credential store, announcing every passkey it stores for the first time. Saves of a known
 * passkey, which every sign-in makes to update its counter, announce nothing.
 */
@CompileStatic
class NotifyingUserCredentialRepository implements UserCredentialRepository, ApplicationEventPublisherAware {

    private final UserCredentialRepository delegate
    private ApplicationEventPublisher publisher

    /**
     * Over Spring Security's JDBC credential store. Built here rather than declared as a bean of its own: two beans
     * of type UserCredentialRepository make Spring Security's WebAuthn configurer fall back to its in-memory one.
     */
    NotifyingUserCredentialRepository(JdbcTemplate jdbcTemplate) {
        this(new JdbcUserCredentialRepository(jdbcTemplate))
    }

    NotifyingUserCredentialRepository(UserCredentialRepository delegate) {
        this.delegate = delegate
    }

    NotifyingUserCredentialRepository(UserCredentialRepository delegate, ApplicationEventPublisher publisher) {
        this.delegate = delegate
        this.publisher = publisher
    }

    @Override
    void setApplicationEventPublisher(ApplicationEventPublisher publisher) {
        this.publisher = publisher
    }

    @Override
    void save(CredentialRecord record) {
        boolean added = delegate.findByCredentialId(record.credentialId) == null
        delegate.save(record)
        if (added) {
            publisher.publishEvent(new PasskeyRegisteredEvent(record.userEntityUserId, record.label))
        }
    }

    @Override
    void delete(Bytes credentialId) {
        delegate.delete(credentialId)
    }

    @Override
    CredentialRecord findByCredentialId(Bytes credentialId) {
        delegate.findByCredentialId(credentialId)
    }

    @Override
    List<CredentialRecord> findByUserId(Bytes userId) {
        delegate.findByUserId(userId)
    }
}
