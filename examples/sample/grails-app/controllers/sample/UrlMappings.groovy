package sample

class UrlMappings {

    static mappings = {
        '/'(controller: 'home', action: 'index', method: 'GET')
        '/admin'(controller: 'home', action: 'admin', method: 'GET')
    }
}
