<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="passwordless.confirmPasskey.title"/> · <pw:appName/></title>
    <pw:passkeyScript/>
</head>
<body>
<div class="row justify-content-center"><div class="col-md-7 col-lg-6">
    <div class="card" data-passkey-scope<pw:csrfAttrs/>>
        <div class="card-body">
            <h1 class="h4 card-title"><g:message code="passwordless.confirmPasskey.title"/></h1>
            <g:if test="${hasPasskey}">
                <p><g:message code="passwordless.confirmPasskey.have"/></p>
                <button type="button" class="btn btn-primary" data-passkey="sign-in"
                        data-passkey-failed="${message(code: 'passwordless.passkeys.failed')}"><g:message code="passwordless.confirmPasskey.confirm"/></button>
            </g:if>
            <g:else>
                <p><g:message code="passwordless.confirmPasskey.none"/></p>
                <div class="mb-3">
                    <label for="passkeyLabel" class="form-label"><g:message code="passwordless.passkeys.label"/></label>
                    <g:textField name="passkeyLabel" id="passkeyLabel" class="form-control" maxlength="100" placeholder="${message(code: 'passwordless.passkeys.labelHint')}"/>
                </div>
                <button type="button" class="btn btn-primary" data-passkey="register" data-passkey-then="sign-in"
                        data-passkey-failed="${message(code: 'passwordless.passkeys.failed')}"><g:message code="passwordless.confirmPasskey.create"/></button>
            </g:else>
            <div class="alert alert-warning mt-3 d-none" data-passkey-error></div>
        </div>
    </div>
</div></div>
</body>
</html>
