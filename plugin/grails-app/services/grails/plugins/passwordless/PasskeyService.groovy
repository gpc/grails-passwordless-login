package grails.plugins.passwordless

import org.springframework.security.web.webauthn.api.Bytes
import org.springframework.security.web.webauthn.api.CredentialRecord
import org.springframework.security.web.webauthn.api.ImmutablePublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.api.PublicKeyCredentialUserEntity
import org.springframework.security.web.webauthn.management.PublicKeyCredentialUserEntityRepository
import org.springframework.security.web.webauthn.management.UserCredentialRepository

/**
 * A user's passkeys: listing and removing them. Passkeys belong to the account, and are stored by Spring Security
 * under its email. Telling people when one is added is the application's, on {@link grails.plugins.passwordless.passkey.PasskeyRegisteredEvent}.
 */
class PasskeyService {

    PublicKeyCredentialUserEntityRepository publicKeyCredentialUserEntityRepository
    UserCredentialRepository userCredentialRepository

    /**
     * Creates the account's WebAuthn identity before their first registration, so authenticators show their name
     * rather than their email.
     */
    void ensureUserEntity(LoginAccount account) {
        if (publicKeyCredentialUserEntityRepository.findByUsername(account.email) == null) {
            publicKeyCredentialUserEntityRepository.save(ImmutablePublicKeyCredentialUserEntity.builder()
                    .name(account.email).id(Bytes.random()).displayName(account.displayName ?: account.email).build())
        }
    }

    List<CredentialRecord> passkeysOf(String email) {
        PublicKeyCredentialUserEntity entity = email ? publicKeyCredentialUserEntityRepository.findByUsername(email) : null
        entity == null ? [] : userCredentialRepository.findByUserId(entity.id).sort { CredentialRecord r -> r.created }
    }

    boolean hasPasskey(String email) {
        !passkeysOf(email).isEmpty()
    }

    /**
     * Removes one of the account's own passkeys.
     *
     * @return false if the credential is not theirs
     */
    boolean deleteOwn(String email, String credentialId) {
        CredentialRecord record = passkeysOf(email).find { CredentialRecord r -> r.credentialId.toBase64UrlString() == credentialId }
        if (record == null) {
            return false
        }
        userCredentialRepository.delete(record.credentialId)
        true
    }

    /**
     * Removes all of an account's passkeys, for an administrator helping someone who lost theirs.
     *
     * @return how many were removed
     */
    int deleteAll(String email) {
        List<CredentialRecord> records = passkeysOf(email)
        records.each { CredentialRecord r -> userCredentialRepository.delete(r.credentialId) }
        records.size()
    }

    /**
     * The account a passkey event concerns, by the WebAuthn user id.
     */
    String emailOf(Bytes userId) {
        publicKeyCredentialUserEntityRepository.findById(userId)?.name
    }
}
