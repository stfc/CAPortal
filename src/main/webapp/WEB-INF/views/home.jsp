<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%@page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Public Home</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for all"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%@ include file="../jspf/styles.jspf" %>
    <%@ include file="../jspf/cookieLaw.jspf" %>
</head>

<body>
<%@ include file="../jspf/header.jspf" %>
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1">
            <h1 id="spacedH1">Welcome to the UK Certification Authority Portal</h1>
            <div class="col-md-offset-1 col-md-4">
                <div class="col-md-offset-1">
                    <img src="${pageContext.request.contextPath}/resources/images/user1.png"
                         class="img-responsive centeredImg" alt="user icon" width="160"/>
                    <h2 class="spacedH2" data-toggle="tooltip" data-placement="top" title="You do not currently have a valid
                                certificate and have not yet applied for one">I am a new user
                    </h2>
                    <div class="form-group">
                        <a class="btn btn-info wrapped-btn"
                           href="${pageContext.request.contextPath}/pub/requestUserCert/submitNewUserCertRequest"
                           data-toggle="tooltip" data-placement="bottom" title="Apply for a new user certificate">Request
                            New User Certificate</a>
                        <a class="btn btn-info wrapped-btn" href="http://ca.grid-support.ac.uk" data-toggle="tooltip"
                           data-placement="bottom"
                           title="Visit the CA Helpdesk for information and assistance">Go To CA Helpdesk</a>
                        <a class="btn btn-info wrapped-btn" href="${pageContext.request.contextPath}/pub/viewralist"
                           data-toggle="tooltip" data-placement="bottom"
                           title="Find your nearest Registration Authority">Find My Local RA</a>
                    </div>
                </div>
            </div>
            <div class="col-md-5">
                <div class="col-md-offset-2">
                    <img src="${pageContext.request.contextPath}/resources/images/user1cert.png"
                         class="img-responsive centeredImg" alt="user + cert" width="160"/>
                    <h2 class="spacedH2" data-toggle="tooltip" data-placement="top" title="You either already have a valid
                                certificate installed into your browser, or have applied for one">I have a grid
                        certificate
                    </h2>
                    <div class="form-group">
                        <a class="btn btn-info wrapped-btn"
                           href="${pageContext.request.contextPath}/pub/downloadCert/requestdownload"
                           data-toggle="tooltip" data-placement="bottom" title="Download your user/host certificate once it has been approved
                                   and you have been e-mailed your certificate serial number">Download a Certificate
                        </a>
                        <a class="btn btn-info wrapped-btn" href="${pageContext.request.contextPath}/cert_owner"
                           data-toggle="tooltip" data-placement="bottom" title="Select a certificate from those installed in your
                                   current browser, or view currently selected certificate">Login / View My Certificate
                        </a>
                        <a class="btn btn-info wrapped-btn"
                           href="${pageContext.request.contextPath}/cert_owner/requestHostCert"
                           data-toggle="tooltip" data-placement="bottom" title="Apply for a new certificate for a host - you
                                   MUST currently have a user certificate installed and selected for this">Request New
                            Host Certificate
                        </a>
                        <a class="btn btn-info wrapped-btn" href="${pageContext.request.contextPath}/cert_owner/renew"
                           data-toggle="tooltip" data-placement="bottom" title="Renew a certificate when you have received
                                   an email instructing you to do so">Renew Certificate</a>
                    </div>
                </div>
            </div>

        </div>
    </div>
</div>
<%@ include file="../jspf/footer.jspf" %>
<script type="text/javascript">
    $(document).ready(function () {
        $('h2').tooltip();
        $('a').tooltip();
    });
</script>

</body>
</html>
