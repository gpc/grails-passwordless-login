<!doctype html>
<html>
<head>
    <meta name="layout" content="main"/>
    <title><g:message code="passwordless.login.title"/> · <pw:appName/></title>
</head>
<body>
<div class="row justify-content-center">
    <div class="col-md-6 col-lg-5">
        <div class="card">
            <div class="card-body">
                <h1 class="h4 card-title"><g:message code="passwordless.confirm.title"/> <pw:appName/></h1>
                <p><g:message code="passwordless.confirm.body"/></p>
                <g:form controller="login" action="redeem" method="POST">
                    <g:hiddenField name="token" value="${token}"/>
                    <button type="submit" class="btn btn-primary btn-lg w-100"><g:message code="passwordless.confirm.button"/></button>
                </g:form>
            </div>
        </div>
    </div>
</div>
</body>
</html>
