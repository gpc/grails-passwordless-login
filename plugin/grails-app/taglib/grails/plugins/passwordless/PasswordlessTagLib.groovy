package grails.plugins.passwordless

import org.springframework.security.web.csrf.CsrfToken
import org.springframework.web.util.HtmlUtils

class PasswordlessTagLib {

    static namespace = 'pw'
    static defaultEncodeAs = [taglib: 'html']
    // Writes markup of its own.
    static encodeAsForTags = [csrfMeta: [taglib: 'none'], csrfAttrs: [taglib: 'none'], passkeyScript: [taglib: 'none']]

    /**
     * The CSRF token as meta tags, for the passkey calls, which post JSON and send the token as a header. Goes in
     * the page's head.
     */
    def csrfMeta(Map attrs) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.name)
        if (token != null) {
            out << '<meta name="_csrf" content="' << HtmlUtils.htmlEscape(token.token) << '"/>'
            out << '<meta name="_csrf_header" content="' << HtmlUtils.htmlEscape(token.headerName) << '"/>'
        }
    }

    /**
     * The CSRF token as data attributes, for the element that scopes the passkey buttons. The plugin's own pages use
     * this, so the application's layout needs nothing.
     */
    def csrfAttrs(Map attrs) {
        CsrfToken token = (CsrfToken) request.getAttribute(CsrfToken.name)
        if (token != null) {
            out << ' data-csrf-header="' << HtmlUtils.htmlEscape(token.headerName) << '" data-csrf="' <<
                    HtmlUtils.htmlEscape(token.token) << '"'
        }
    }

    /**
     * The passkey script. Served from the plugin, so an application needs no asset of its own.
     */
    def passkeyScript(Map attrs) {
        out << '<script src="' << HtmlUtils.htmlEscape(g.createLink(controller: 'passkey', action: 'script', method: 'GET') as String) << '"></script>'
    }

    /**
     * The application's name, passwordless.app-name.
     */
    def appName(Map attrs) {
        out << grailsApplication.config.getProperty('passwordless.app-name', String, 'Login')
    }
}
