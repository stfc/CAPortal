<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%@ page session="false" %>
<!DOCTYPE html>
<html>
<head>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Access Denied</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Access denied page for failed authentication"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <%@ include file="../../jspf/cookieLaw.jspf" %>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1 col-xs-11">
            <h1>Access Denied</h1>
            <p class="text-info">You have been denied access due to one of the following reasons:</p>
            <ul>
                <li>You do not have a valid certificate issued from this CA loaded in your browser<br/>
                    (Some pages e.g. 'Request a Host Certificate' require you have a valid
                    user certificate loaded into your browser)
                </li>
                <li>Your certificate has expired</li>
                <li>Your certificate does not grant you sufficient rights to access the resource<br/>
                    (Some pages e.g. 'RAOP Home' require a valid RA operator certificate)
                </li>
            </ul>
            <p class="text-info">
                If you have loaded your certificate into your browser, you may need to
                refresh the SSL context by restarting the browser (before you restart the
                browser close the tab first, then restart). On restart, you should
                be prompted to select your certificate.
            </p>
            <br/>
            <br/>

            <h2>Known Issues</h2>
            <ul>
                <li><a href="#chrome">Chrome</a></li>
                <li><a href="#firefox">Firefox</a></li>
                <li><a href="#safari">Safari</a></li>
            </ul>
            <br/>

            <a id="chrome"><strong>Chrome</strong></a> If you are sure you have loaded your certificate into your
            browser, you may
            need to restart Chrome so that you are (re)prompted to select your certificate.
            Note, by default Chrome does not shut down and runs in the background when you
            close the Chrome window. You may need to fully close Chrome by un-checking
            the 'Continue running background apps' checkbox in 'Settings | Show Advanced Settings':
            <br/> <br/>
            <img src="${pageContext.request.contextPath}/resources/images/chrome1.jpg" class="img-responsive"
                 alt="chome settings" style="width: 650px;"/>
            <br/> <br/>

            <a id="firefox"><strong>Firefox</strong></a>
            If after loading multiple certificates into your browser you are not
            prompted with a choice when accessing the portal, please close the tab(s) open to the portal,
            clear your recent browser cache as detailed below and then restart your browser:
            <br/> <br/>
            <img src="${pageContext.request.contextPath}/resources/images/ffclear1.png" class="img-responsive"
                 alt="ff settings 1"/>
            <br/> <br/>
            <img src="${pageContext.request.contextPath}/resources/images/ffclear2.png" class="img-responsive"
                 alt="ff settings 2"/>
            <br/> <br/>
            Note that you may also need 'Ask me every time' enabled to ensure the correct certificate is in use, found
            in the Advanced tab of Firefox Options (under 'Certificates'):
            <br/> <br/>
            <img src="${pageContext.request.contextPath}/resources/images/ffprompt.png" class="img-responsive"
                 alt="ff prompt"/>

            <br/>
            <br/>

            <p><a id="safari"><strong>Safari</strong></a>
                If you have a client certificate in your Safari keychain, including certificates <strong>not</strong>
                issued from
                the UK CA, then Safari always (wrongly) insists the user selects a certificate (cancelling leads to
                denied page).
                This is because Safari wrongly treats clientAuth="want" (i.e. optional) as clientAuth="Required":
                See
                <a href="http://superuser.com/questions/521919/safari-forces-user-to-select-client-certificate-even-if-it-is-optional">Bug</a>
                & <a href="http://lists.apple.com/archives/apple-cdsa/2012/Dec/msg00005.html">Bug</a>.
            </p>
            <p>Additionally, Safari 'remembers' SSL certificate choice and will not
                prompt again after selection. See <a
                        href="https://discussions.apple.com/message/19828596#19828596">Here</a>.</p>
            <p>--Workaround--: <br/>Use another browser, remove certificates from keychain, or select suitable UK CA
                client certificate.<br/>
                Can manually assign a certificate from the keychain as <a
                        href="http://support.apple.com/kb/HT1679">Here</a>.
            </p>
            <br/><br/>
        </div>
    </div>
</div>


<%@ include file="../../jspf/footer.jspf" %>
<%--<jsp:include page="common/footer.jsp" />--%>
</body>
</html>