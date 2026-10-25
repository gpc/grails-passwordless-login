// Passkeys against Spring Security's WebAuthn endpoints, on the same JSON contract as Spring's own script.
// Buttons opt in with data-passkey="sign-in" or data-passkey="register"; messages go to [data-passkey-error].
(function () {
    'use strict';

    const b64url = {
        encode(buffer) {
            return btoa(String.fromCharCode(...new Uint8Array(buffer))).replace(/=/g, '').replace(/\+/g, '-').replace(/\//g, '_');
        },
        decode(text) {
            const binary = atob(text.replace(/-/g, '+').replace(/_/g, '/'));
            return Uint8Array.from(binary, c => c.charCodeAt(0)).buffer;
        }
    };

    function meta(name) {
        const element = document.querySelector('meta[name="' + name + '"]');
        return element ? element.content : null;
    }

    // The CSRF token comes from data attributes on the passkey scope (pw:csrfAttrs), or from meta tags (pw:csrfMeta).
    function csrf() {
        const scope = document.querySelector('[data-csrf-header]');
        if (scope) {
            return {header: scope.dataset.csrfHeader, token: scope.dataset.csrf};
        }
        return {header: meta('_csrf_header'), token: meta('_csrf')};
    }

    async function post(url, body) {
        const headers = {'Content-Type': 'application/json'};
        const token = csrf();
        if (token.header) {
            headers[token.header] = token.token;
        }
        const response = await fetch(url, {method: 'POST', headers, body: body ? JSON.stringify(body) : undefined});
        if (!response.ok) {
            throw new Error('HTTP ' + response.status);
        }
        return response.json();
    }

    async function signIn() {
        const options = await post('/webauthn/authenticate/options');
        options.challenge = b64url.decode(options.challenge);
        options.allowCredentials = (options.allowCredentials || []).map(c => ({...c, id: b64url.decode(c.id)}));
        const credential = await navigator.credentials.get({publicKey: options});
        const r = credential.response;
        const result = await post('/login/webauthn', {
            id: credential.id,
            rawId: b64url.encode(credential.rawId),
            response: {
                authenticatorData: b64url.encode(r.authenticatorData),
                clientDataJSON: b64url.encode(r.clientDataJSON),
                signature: b64url.encode(r.signature),
                userHandle: r.userHandle ? b64url.encode(r.userHandle) : undefined
            },
            credType: credential.type,
            clientExtensionResults: credential.getClientExtensionResults(),
            authenticatorAttachment: credential.authenticatorAttachment
        });
        if (!result.authenticated) {
            throw new Error('Not signed in');
        }
        window.location.href = result.redirectUrl || '/';
    }

    async function register(label) {
        const options = await post('/webauthn/register/options');
        options.challenge = b64url.decode(options.challenge);
        options.user = {...options.user, id: b64url.decode(options.user.id)};
        options.excludeCredentials = (options.excludeCredentials || []).map(c => ({...c, id: b64url.decode(c.id)}));
        const credential = await navigator.credentials.create({publicKey: options});
        const r = credential.response;
        const result = await post('/webauthn/register', {
            publicKey: {
                label,
                credential: {
                    id: credential.id,
                    rawId: b64url.encode(credential.rawId),
                    response: {
                        attestationObject: b64url.encode(r.attestationObject),
                        clientDataJSON: b64url.encode(r.clientDataJSON),
                        transports: r.getTransports ? r.getTransports() : []
                    },
                    type: credential.type,
                    clientExtensionResults: credential.getClientExtensionResults(),
                    authenticatorAttachment: credential.authenticatorAttachment
                }
            }
        });
        if (!result.success) {
            throw new Error('Not registered');
        }
    }

    function showError(button, text) {
        const scope = button.closest('[data-passkey-scope]') || document;
        const error = scope.querySelector('[data-passkey-error]');
        if (error) {
            error.textContent = text;
            error.classList.remove('d-none');
        }
    }

    document.addEventListener('DOMContentLoaded', function () {
        const buttons = document.querySelectorAll('[data-passkey]');
        if (!window.PublicKeyCredential) {
            buttons.forEach(b => b.closest('[data-passkey-scope]')?.classList.add('d-none'));
            return;
        }
        buttons.forEach(button => button.addEventListener('click', async function () {
            button.disabled = true;
            try {
                if (button.dataset.passkey === 'sign-in') {
                    await signIn();
                } else {
                    const input = document.getElementById(button.dataset.passkeyLabel || 'passkeyLabel');
                    await register((input && input.value.trim()) || 'Adgangsnøgle');
                    if (button.dataset.passkeyThen === 'sign-in') {
                        await signIn();
                    } else {
                        window.location.reload();
                    }
                }
            } catch (e) {
                console.error(e);
                showError(button, button.dataset.passkeyFailed || 'Det lykkedes ikke. Prøv igen.');
                button.disabled = false;
            }
        }));
    });
})();
