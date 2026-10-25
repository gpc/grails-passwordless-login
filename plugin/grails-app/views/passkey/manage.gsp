<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="passwordless.passkeys.title"/> · <pw:appName/></title>
    <pw:passkeyScript/>
</head>
<body>
<div class="row"><div class="col-lg-8">
    <h1 class="h4 mb-3"><g:message code="passwordless.passkeys.title"/></h1>
    <p class="text-body-secondary"><g:message code="passwordless.passkeys.intro"/></p>

    <g:if test="${flash.error}"><div class="alert alert-warning">${flash.error}</div></g:if>
    <g:if test="${flash.message}"><div class="alert alert-info">${flash.message}</div></g:if>

    <g:if test="${passkeys}">
        <ul class="list-group mb-4">
            <g:each in="${passkeys}" var="p">
                <li class="list-group-item d-flex justify-content-between align-items-center">
                    <span>${p.label}<br><small class="text-body-secondary"><g:message code="passwordless.passkeys.created"/> <g:formatDate date="${java.util.Date.from(p.created)}" format="dd-MM-yyyy"/><g:if test="${p.lastUsed}">, <g:message code="passwordless.passkeys.lastUsed"/> <g:formatDate date="${java.util.Date.from(p.lastUsed)}" format="dd-MM-yyyy"/></g:if></small></span>
                    <g:if test="${verified}">
                        <g:form controller="passkey" action="delete" method="POST" class="m-0">
                            <g:hiddenField name="credentialId" value="${p.credentialId.toBase64UrlString()}"/>
                            <button type="submit" class="btn btn-outline-danger btn-sm"><g:message code="passwordless.passkeys.remove"/></button>
                        </g:form>
                    </g:if>
                </li>
            </g:each>
        </ul>
    </g:if>
    <g:else>
        <p><g:message code="passwordless.passkeys.none"/></p>
    </g:else>

    <g:if test="${mayRegister}">
        <div class="card" data-passkey-scope<pw:csrfAttrs/>>
            <div class="card-body">
                <h2 class="h6"><g:message code="passwordless.passkeys.addTitle"/></h2>
                <div class="mb-3">
                    <label for="passkeyLabel" class="form-label"><g:message code="passwordless.passkeys.label"/></label>
                    <g:textField name="passkeyLabel" id="passkeyLabel" class="form-control" maxlength="100" placeholder="${message(code: 'passwordless.passkeys.labelHint')}"/>
                </div>
                <button type="button" class="btn btn-primary" data-passkey="register"
                        data-passkey-failed="${message(code: 'passwordless.passkeys.failed')}"><g:message code="passwordless.passkeys.add"/></button>
                <div class="alert alert-warning mt-3 d-none" data-passkey-error></div>
            </div>
        </div>
    </g:if>
    <g:else>
        <div class="alert alert-info"><g:message code="passwordless.passkeys.confirmFirst"/></div>
    </g:else>
</div></div>
</body>
</html>
