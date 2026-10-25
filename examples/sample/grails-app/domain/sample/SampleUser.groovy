package sample

import grails.compiler.GrailsCompileStatic

@GrailsCompileStatic
class SampleUser {

    String email
    String displayName
    boolean enabled = true
    boolean admin

    static constraints = {
        email nullable: false, blank: false, email: true, maxSize: 254, unique: true
        displayName nullable: false, blank: false, maxSize: 200
    }

    static mapping = {
        table 'sample_user'
    }

    def beforeValidate() {
        email = email?.trim()?.toLowerCase(Locale.ROOT)
    }
}
