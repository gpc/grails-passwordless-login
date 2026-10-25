package grails.plugins.passwordless.passkey

import spock.lang.Specification

class PasskeySignInHandlerSpec extends Specification {

    void "only a path on this application is a place to return to: #target"() {
        expect:
        PasskeySignInHandler.isLocalPath(target) == local

        where:
        target                   | local
        '/admin'                 | true
        '/admin/members?x=1'     | true
        null                     | false
        ''                       | false
        'admin'                  | false
        '//evil.example'         | false
        '/\\evil.example'        | false
        'https://evil.example/'  | false
        '/redirect?to=http://x/' | false
    }
}
