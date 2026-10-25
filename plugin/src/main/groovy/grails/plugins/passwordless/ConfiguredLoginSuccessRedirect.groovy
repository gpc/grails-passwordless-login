package grails.plugins.passwordless

import groovy.transform.CompileStatic

@CompileStatic
class ConfiguredLoginSuccessRedirect implements LoginSuccessRedirect {

    final String url

    ConfiguredLoginSuccessRedirect(String url) {
        this.url = url
    }

    @Override
    String urlFor(LoginPrincipal principal, String context) {
        url
    }
}
