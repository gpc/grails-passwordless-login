package sample

import grails.gorm.transactions.Transactional
import grails.plugins.passwordless.LoginAccount
import grails.plugins.passwordless.LoginAccountLookup

/**
 * The application's side of the plugin: who can sign in. A disabled user is not found.
 */
@Transactional(readOnly = true)
class SampleUserLookupService implements LoginAccountLookup {

    @Override
    LoginAccount findByEmail(String email) {
        account(SampleUser.findByEmailAndEnabled(email, true))
    }

    @Override
    LoginAccount findById(String id) {
        account(id?.isLong() ? SampleUser.findByIdAndEnabled(id.toLong(), true) : null)
    }

    private static LoginAccount account(SampleUser user) {
        user == null ? null : new LoginAccount(user.id.toString(), user.email, user.displayName)
    }
}
