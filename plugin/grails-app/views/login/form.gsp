<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="passwordless.login.title"/> · <pw:appName/></title>
    <pw:passkeyScript/>
</head>
<body>
<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <g:if test="${loggedOut}">
            <div class="alert alert-info"><g:message code="passwordless.login.loggedOut"/></div>
        </g:if>
        <g:if test="${invalid}">
            <div class="alert alert-warning"><g:message code="passwordless.login.invalid"/></div>
        </g:if>
        <div class="card">
            <div class="card-body">
                <h1 class="h4 card-title"><g:message code="passwordless.login.title"/></h1>
                <p class="text-body-secondary"><g:message code="passwordless.login.intro"/></p>
                <g:form controller="login" action="send" method="POST">
                    <g:if test="${context}"><g:hiddenField name="context" value="${context}"/></g:if>
                    <div class="mb-3">
                        <label for="email" class="form-label"><g:message code="passwordless.login.email"/></label>
                        <g:field type="email" name="email" id="email" class="form-control" autocomplete="email" required="" autofocus=""/>
                    </div>
                    <button type="submit" class="btn btn-primary"><g:message code="passwordless.login.send"/></button>
                </g:form>
                <div class="mt-4 pt-3 border-top" data-passkey-scope<pw:csrfAttrs/>>
                    <p class="text-body-secondary small mb-2"><g:message code="passwordless.login.passkeyHint"/></p>
                    <button type="button" class="btn btn-outline-primary" data-passkey="sign-in"
                            data-passkey-failed="${message(code: 'passwordless.login.passkeyFailed')}"><g:message code="passwordless.login.passkey"/></button>
                    <div class="alert alert-warning mt-3 d-none" data-passkey-error></div>
                </div>
            </div>
        </div>
    </div>
</div>
</body>
</html>
