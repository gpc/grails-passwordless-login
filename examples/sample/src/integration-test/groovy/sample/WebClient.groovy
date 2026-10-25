package sample

import java.net.http.HttpClient
import java.net.http.HttpRequest
import java.net.http.HttpResponse
import java.nio.charset.StandardCharsets

/**
 * A browser stand-in: follows no redirects and keeps cookies itself. java.net's cookie handling would drop the
 * session cookie, which is Secure, on the test server's plain http.
 */
class WebClient {

    final String baseUrl
    final Map<String, String> cookies = [:]
    final HttpClient client = HttpClient.newBuilder().followRedirects(HttpClient.Redirect.NEVER).build()

    WebClient(int port) {
        baseUrl = "http://localhost:${port}"
    }

    HttpResponse<String> get(String path) {
        send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET())
    }

    HttpResponse<byte[]> getBytes(String path) {
        send(HttpRequest.newBuilder(URI.create(baseUrl + path)).GET(), HttpResponse.BodyHandlers.ofByteArray())
    }

    HttpResponse<String> post(String path, Map<String, String> form) {
        String body = form.collect { k, v -> "${enc(k)}=${enc(v ?: '')}" }.join('&')
        send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header('Content-Type', 'application/x-www-form-urlencoded')
                .POST(HttpRequest.BodyPublishers.ofString(body)))
    }

    /**
     * Posts fields that may repeat, such as several checked boxes of the same name.
     */
    HttpResponse<String> postFields(String path, List<List<String>> fields) {
        String body = fields.collect { List<String> f -> "${enc(f[0])}=${enc(f[1] ?: '')}" }.join('&')
        send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header('Content-Type', 'application/x-www-form-urlencoded')
                .POST(HttpRequest.BodyPublishers.ofString(body)))
    }

    /**
     * Posts JSON with the CSRF token as a header, as the passkey script does.
     */
    HttpResponse<String> postJson(String path, String json, String csrfToken) {
        HttpRequest.Builder builder = HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header('Content-Type', 'application/json')
                .POST(json != null ? HttpRequest.BodyPublishers.ofString(json) : HttpRequest.BodyPublishers.noBody())
        if (csrfToken) {
            builder.header('X-CSRF-TOKEN', csrfToken)
        }
        send(builder)
    }

    /**
     * The CSRF token from a page's meta tags.
     */
    static String metaCsrfToken(String html) {
        def matcher = html =~ /<meta name="_csrf" content="([^"]*)"/
        assert matcher.find(): 'No CSRF meta tag on the page'
        matcher.group(1)
    }

    /**
     * The CSRF token of the login plugin's pages, which carry it as data attributes on the passkey buttons' scope.
     */
    static String passkeyCsrfToken(String html) {
        def matcher = html =~ /data-csrf="([^"]*)"/
        assert matcher.find(): 'No CSRF data attribute on the page'
        matcher.group(1)
    }

    HttpResponse<byte[]> postBytes(String path, Map<String, String> form) {
        String body = form.collect { k, v -> "${enc(k)}=${enc(v ?: '')}" }.join('&')
        send(HttpRequest.newBuilder(URI.create(baseUrl + path))
                .header('Content-Type', 'application/x-www-form-urlencoded')
                .POST(HttpRequest.BodyPublishers.ofString(body)), HttpResponse.BodyHandlers.ofByteArray())
    }

    /**
     * Posts a form the way a browser would after loading the page that holds it: with that page's CSRF token.
     */
    HttpResponse<String> submit(String pagePath, String actionPath, Map<String, String> form) {
        HttpResponse<String> page = get(pagePath)
        assert page.statusCode() == 200: "GET ${pagePath} answered ${page.statusCode()}"
        post(actionPath, form + [_csrf: csrfToken(page.body())])
    }

    /**
     * Posts a multipart form with one file, as a browser's file upload does, with the CSRF token from the page that
     * holds the form.
     */
    HttpResponse<String> upload(String pagePath, String actionPath, String fileField, String fileName, String contentType, byte[] content) {
        HttpResponse<String> page = get(pagePath)
        assert page.statusCode() == 200: "GET ${pagePath} answered ${page.statusCode()}"
        String boundary = "----sample${UUID.randomUUID()}"
        ByteArrayOutputStream body = new ByteArrayOutputStream()
        body << "--${boundary}\r\nContent-Disposition: form-data; name=\"_csrf\"\r\n\r\n${csrfToken(page.body())}\r\n".getBytes(StandardCharsets.UTF_8)
        body << ("--${boundary}\r\nContent-Disposition: form-data; name=\"${fileField}\"; filename=\"${fileName}\"\r\n" +
                "Content-Type: ${contentType}\r\n\r\n").getBytes(StandardCharsets.UTF_8)
        body << content
        body << "\r\n--${boundary}--\r\n".getBytes(StandardCharsets.UTF_8)
        send(HttpRequest.newBuilder(URI.create(baseUrl + actionPath))
                .header('Content-Type', "multipart/form-data; boundary=${boundary}")
                .POST(HttpRequest.BodyPublishers.ofByteArray(body.toByteArray())))
    }

    static String csrfToken(String html) {
        def input = html =~ /<input[^>]*name="_csrf"[^>]*>/
        assert input.find(): 'No CSRF token on the page'
        (input.group() =~ /value="([^"]*)"/).with { it.find() ? it.group(1) : null }
    }

    String getSessionId() {
        cookies['sample']
    }

    private HttpResponse<String> send(HttpRequest.Builder builder) {
        send(builder, HttpResponse.BodyHandlers.ofString())
    }

    private <T> HttpResponse<T> send(HttpRequest.Builder builder, HttpResponse.BodyHandler<T> handler) {
        if (cookies) {
            builder.header('Cookie', cookies.collect { k, v -> "${k}=${v}" }.join('; '))
        }
        HttpResponse<T> response = client.send(builder.build(), handler)
        response.headers().allValues('Set-Cookie').each { String header ->
            String pair = header.split(';')[0]
            int eq = pair.indexOf('=')
            String name = pair.substring(0, eq)
            String value = pair.substring(eq + 1)
            boolean expired = header.toLowerCase().contains('max-age=0') || value.isEmpty()
            expired ? cookies.remove(name) : cookies.put(name, value)
        }
        response
    }

    private static String enc(String s) {
        URLEncoder.encode(s, StandardCharsets.UTF_8)
    }
}
