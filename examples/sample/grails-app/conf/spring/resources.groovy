import sample.LinkInbox
import sample.PasskeyLog
import sample.SampleSessionPolicy

beans = {
    // What the plugin asks of an application
    springConfig.addAlias('loginAccountLookup', 'sampleUserLookupService')
    springConfig.addAlias('loginPrincipalFactory', 'samplePrincipalService')
    linkInbox(LinkInbox)
    springConfig.addAlias('loginLinkSender', 'linkInbox')
    sessionPolicy(SampleSessionPolicy)

    passkeyLog(PasskeyLog)
}
