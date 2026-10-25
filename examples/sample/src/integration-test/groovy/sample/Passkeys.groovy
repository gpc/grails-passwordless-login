package sample

import com.webauthn4j.data.AttestationConveyancePreference
import com.webauthn4j.data.AuthenticatorAssertionResponse
import com.webauthn4j.data.AuthenticatorAttestationResponse
import com.webauthn4j.data.AuthenticatorSelectionCriteria
import com.webauthn4j.data.PublicKeyCredential
import com.webauthn4j.data.PublicKeyCredentialCreationOptions
import com.webauthn4j.data.PublicKeyCredentialDescriptor
import com.webauthn4j.data.PublicKeyCredentialParameters
import com.webauthn4j.data.PublicKeyCredentialRequestOptions
import com.webauthn4j.data.PublicKeyCredentialRpEntity
import com.webauthn4j.data.PublicKeyCredentialType
import com.webauthn4j.data.PublicKeyCredentialUserEntity
import com.webauthn4j.data.ResidentKeyRequirement
import com.webauthn4j.data.UserVerificationRequirement
import com.webauthn4j.data.attestation.statement.COSEAlgorithmIdentifier
import com.webauthn4j.data.client.Origin
import com.webauthn4j.data.client.challenge.DefaultChallenge
import com.webauthn4j.test.authenticator.webauthn.PackedAuthenticator
import com.webauthn4j.test.authenticator.webauthn.WebAuthnAuthenticatorAdaptor
import com.webauthn4j.test.client.ClientPlatform

import groovy.json.JsonOutput
import groovy.json.JsonSlurper

import java.net.http.HttpResponse
import java.util.concurrent.ConcurrentHashMap

/**
 * A software passkey per person, driven over plain HTTP against Spring Security's WebAuthn endpoints, with the same
 * JSON the browser script sends. Lets specs register passkeys and sign in with them without a browser.
 */
class Passkeys {

    /** grails.serverURL in the test environment, which the relying party is derived from. */
    static final String ORIGIN = 'http://localhost'

    private static final Map<String, ClientPlatform> AUTHENTICATORS = new ConcurrentHashMap<>()
    private static final Map<String, List<byte[]>> CREDENTIALS = new ConcurrentHashMap<>()

    static ClientPlatform authenticatorOf(String email) {
        AUTHENTICATORS.computeIfAbsent(email) { new ClientPlatform(new Origin(ORIGIN), new WebAuthnAuthenticatorAdaptor(new PackedAuthenticator())) }
    }

    /**
     * Registers a passkey for the person signed in on this client.
     */
    static HttpResponse<String> register(WebClient web, String email, String label = 'Testnøgle') {
        String csrf = WebClient.passkeyCsrfToken(web.get('/login').body())
        HttpResponse<String> optionsResponse = web.postJson('/webauthn/register/options', null, csrf)
        if (optionsResponse.statusCode() != 200) {
            return optionsResponse
        }
        Map o = (Map) new JsonSlurper().parseText(optionsResponse.body())
        PublicKeyCredentialCreationOptions options = new PublicKeyCredentialCreationOptions(
                new PublicKeyCredentialRpEntity((String) o.rp.id, (String) o.rp.name),
                new PublicKeyCredentialUserEntity(decode((String) o.user.id), (String) o.user.name, (String) o.user.displayName),
                new DefaultChallenge(decode((String) o.challenge)),
                [new PublicKeyCredentialParameters(PublicKeyCredentialType.PUBLIC_KEY, COSEAlgorithmIdentifier.ES256)],
                60000L, [],
                new AuthenticatorSelectionCriteria(null, ResidentKeyRequirement.REQUIRED, UserVerificationRequirement.PREFERRED),
                null, AttestationConveyancePreference.NONE, null)
        PublicKeyCredential<AuthenticatorAttestationResponse, ?> credential = authenticatorOf(email).create(options)
        HttpResponse<String> registered = web.postJson('/webauthn/register', JsonOutput.toJson([publicKey: [
                label     : label,
                credential: [
                        id                     : encode(credential.rawId),
                        rawId                  : encode(credential.rawId),
                        response               : [
                                attestationObject: encode(credential.response.attestationObject),
                                clientDataJSON   : encode(credential.response.clientDataJSON),
                                transports       : ['internal'],
                        ],
                        type                   : 'public-key',
                        clientExtensionResults : [:],
                        authenticatorAttachment: 'platform',
                ],
        ]]), csrf)
        if (registered.statusCode() == 200) {
            CREDENTIALS.computeIfAbsent(email) { [] } << credential.rawId
        }
        registered
    }

    /**
     * Signs in with the person's passkey on this client. The session is then passkey-verified.
     */
    static HttpResponse<String> signIn(WebClient web, String email) {
        String csrf = WebClient.passkeyCsrfToken(web.get('/login').body())
        HttpResponse<String> optionsResponse = web.postJson('/webauthn/authenticate/options', null, csrf)
        assert optionsResponse.statusCode() == 200: "authenticate options answered ${optionsResponse.statusCode()}"
        Map o = (Map) new JsonSlurper().parseText(optionsResponse.body())
        PublicKeyCredentialRequestOptions options = new PublicKeyCredentialRequestOptions(
                new DefaultChallenge(decode((String) o.challenge)), 60000L, (String) o.rpId,
                (CREDENTIALS[email] ?: []).collect { byte[] id -> new PublicKeyCredentialDescriptor(PublicKeyCredentialType.PUBLIC_KEY, id, null) },
                UserVerificationRequirement.PREFERRED, null)
        PublicKeyCredential<AuthenticatorAssertionResponse, ?> assertion = authenticatorOf(email).get(options)
        web.postJson('/login/webauthn', JsonOutput.toJson([
                id                     : encode(assertion.rawId),
                rawId                  : encode(assertion.rawId),
                response               : [
                        authenticatorData: encode(assertion.response.authenticatorData),
                        clientDataJSON   : encode(assertion.response.clientDataJSON),
                        signature        : encode(assertion.response.signature),
                        userHandle       : assertion.response.userHandle ? encode(assertion.response.userHandle) : null,
                ],
                credType               : 'public-key',
                clientExtensionResults : [:],
                authenticatorAttachment: 'platform',
        ]), csrf)
    }

    static boolean hasRegistered(String email) {
        CREDENTIALS[email]
    }

    private static String encode(byte[] bytes) {
        Base64.urlEncoder.withoutPadding().encodeToString(bytes)
    }

    private static byte[] decode(String text) {
        Base64.urlDecoder.decode(text)
    }
}
