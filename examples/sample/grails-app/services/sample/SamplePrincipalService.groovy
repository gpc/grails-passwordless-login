package sample

import grails.gorm.transactions.Transactional
import grails.plugins.passwordless.LoginAccount
import grails.plugins.passwordless.LoginPrincipal
import grails.plugins.passwordless.LoginPrincipalFactory

@Transactional(readOnly = true)
class SamplePrincipalService implements LoginPrincipalFactory {

    @Override
    LoginPrincipal principalFor(LoginAccount account, boolean passkeyVerified) {
        SampleUser user = SampleUser.get(account.id.toLong())
        new SamplePrincipal(user.id, user.email, user.displayName, user.admin, passkeyVerified)
    }
}
