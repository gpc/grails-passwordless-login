# Grails Passwordless Login

Magic link and passkey (WebAuthn) sign-in for Grails 8 applications, on plain Spring Security 7. 

The plugin owns the mechanics: hashed single-use login tokens, a confirmation page that keeps mail scanners from
spending a link, rate limits, passkey registration and sign-in, the session, and default login and passkey pages
(Danish and English). The application supplies who the users are (`LoginAccountLookup`), how a link reaches them
(`LoginLinkSender`) and what the session principal holds (`LoginPrincipalFactory`). See `docs/src/docs/usage`.

## Build

JDK 21 (see `.sdkmanrc`; the Groovy compiler reads the Java version from it) and Gradle 9.8.1, both from the wrapper
and SDKMAN.

```bash
./gradlew :grails-passwordless-login:check
./gradlew :grails-passwordless-login:publishToMavenLocal
```

Applications resolve `io.github.gpc:grails-passwordless-login:1.0.0-SNAPSHOT` from mavenLocal until it is published.

## Tests

`plugin/` has unit specs. The integration specs are in `examples/sample`, a small application that uses the plugin as an
application would (PostgreSQL through Testcontainers, so Docker is needed, with the plugin's own Liquibase changelog):

- `MagicLinkSpec`: what is stored (only the hash), who gets a link, single use (also with simultaneous redemptions),
  expiry, the confirmation page that keeps mail scanners from spending a link, the context an application attaches,
  the session a sign-in ends in and its idle timeout, and clean-up of old tokens.
- `RateLimitSpec`: five links an hour per account, answered silently; twenty requests an hour per address, answered
  with 429; separate accounts and addresses; the sliding window.
- `PasskeySpec`: registering and signing in with a software authenticator, the event an application listens to, who may
  add or remove a passkey, a page that asks for a confirmed passkey, a disabled account, and the script and CSRF token
  the pages carry.

```bash
./gradlew check
```
