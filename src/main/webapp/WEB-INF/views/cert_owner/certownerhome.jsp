<%@page contentType="text/html" pageEncoding="windows-1252"%>
<%@page session="false"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<!DOCTYPE html>

<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
        <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/> 
        <title>Cert Owner Home</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
        <meta name="description" content="Home page for UK CA certificate owners"/>
        <meta name="author" content="David Meredith"/>
          <!-- Styles -->
        <%--<jsp:include page="../common/styles.jsp" />--%>
        <%@ include file="../../jspf/styles.jspf" %>
        <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet" />
    </head>

    <body>
        <%--<jsp:include page="../common/header.jsp"/>--%> 
        <%@ include file="../../jspf/header.jspf" %>
          <!-- Wrap all page content here -->
            <div id="wrap">
                  <div class="row">
                        <div class="col-xs-offset-1">

                    <h3>Your Certificate Details</h3>
                    <c:if test="${not empty emailUpdateOkMessage}">
                        <div class="success">${emailUpdateOkMessage}</div>
                    </c:if>
                    <c:if test="${not empty emailUpdateFailMessage}">
                        <div class="error">${emailUpdateFailMessage}</div>
                    </c:if>
                    <div class="col-xs-11 col-lg-10">          
                        <table class="table table-hover table-condensed">
                            <thead>
                                <tr>
                                    <th>Certificate Attribute</th>
                                    <th>Value</th>
                                </tr>
                            </thead>
                            <tbody>
                            <tr>
                                 <td>Serial Number (cert_key)</td>
                                <td>${certificateRow.cert_key}, (hex: ${certHexSerial})</td>
                            </tr>
                            <tr>	
                                <td>Common Name (CN)</td><td>${certificateRow.cn}</td>
                            </tr>
                             <tr>	
                                <td>Distinguished Name (DN)</td><td>${cert.subjectDN}</td>
                            </tr>
                             <tr>
                                 <td>Issuer DN</td>
                                 <td>${cert.issuerDN}</td>
                            </tr>
                            <tr>
                                <td>Email</td> 
                                <td><a href="mailto:${certificateRow.email}" id="currEmail">${certificateRow.email}</a>
                                    <c:if test="${fn:contains(certificateRow.cn, '.')}">
                                        <form:form method="post" action="${pageContext.request.contextPath}/cert_owner/changemail"> 
                                            <div id="inputEmail">
                                                <input name="email" id="emailInputText" value="${certificateRow.email}"/><span></span>
                                            </div>
                                                <button type="button" id="changeEmail" class="btn btn-primary btn-xs">Change host email</button>
                                                <button type="submit" id="saveEmail" class="btn btn-primary btn-xs" 
                                                    onclick="return confirm('Are you sure you want to change the host email address?')">Save</button>
                                                <a onclick="location.reload(true)">Cancel</a>
                                        </form:form>
                                    </c:if>
                                </td>
                            </tr>
                            <tr>
                                <td>Status</td>
                                <td><b>
                                    <c:if test="${certificateRow.status == 'VALID'}">
                                        <font color="green">${certificateRow.status}</font>
                                    </c:if>
                                    <c:if test="${certificateRow.status != 'VALID'}">
                                        <font color="red">${certificateRow.status}</font>
                                    </c:if>
                                  </b></td>
                            </tr>
                            <tr>
                                <td>Not Before</td><td>${cert.notBefore}</td>
                            </tr>
                            <tr>
                                <td>Not After</td> <td>${cert.notAfter}</td>
                            </tr>	

                            <tr>
                                <td>Signature Algorithm</td>
                                <td>${cert.sigAlgName}</td>
                            </tr>
                            <tr>
                                <td>Type</td>
                                <td>${cert.type}</td>
                            </tr>
                            <tr>
                                <td>Version</td>
                                <td>${cert.version}</td>
                            </tr>
                            </tbody>
                        </table>

                        <h3>Your Roles with the UK CA</h3>
                        <table class="table table-hover table-condensed">
                            <c:forEach var="role" items="${caUser.authorities}">
                                <tr>
                                    <c:if test="${role == 'ROLE_CERTOWNER'}">
                                        <td class="text-success"><b>${role}</b></td>
                                        <td>You have a certificate issued by the UK CA.</td>
                                    </c:if>
                                </tr>
                                <tr>
                                    <c:if test="${role == 'ROLE_RAOP'}">
                                        <td class="text-info"><b>${role}</b></td>
                                        <td>You have <span class="text-info">RA Operator</span> privileges.</td>
                                    </c:if>
                                </tr>
                                <tr>
                                    <c:if test="${role == 'ROLE_CAOP'}">
                                        <td class="text-danger"><b>${role}</b></td>
                                        <td>You have <span class="text-danger">CA Operator</span> privileges.</td>
                                    </c:if>
                                </tr>
                            </c:forEach>
                        </table>
                    </div>
                   
                    <br/><br/>

                    <%-- <p>
                    Your principal object is....: <%= request.getUserPrincipal() %>
                    </p> --%>
                </div>
            </div>
        </div>
        <%@ include file="../../jspf/footer.jspf" %>
        <script type="text/javascript">
            function emailValid() {
                var inputEmail = $("#emailInputText"); 
                var currEmail = $("#currEmail").text();
                //adds error text and disables save button if not an email address or email same as current email
                if(inputEmail.val().match(/^[_A-Za-z0-9-\+]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9]+)*(\.[A-Za-z]{2,})$/) === null || inputEmail.val() === currEmail) {
                    $(inputEmail).addClass("error").next().text("Invalid email");
                    document.getElementById("saveEmail").disabled = true;
                    return false; 
                } else {
                    $(inputEmail).removeClass("error").next().text("");
                    document.getElementById("saveEmail").disabled = false;
                    return true; 
                }
            }
            function enableSubmit() {
                if(emailValid()){
                    return true; 
                } else {
                    return false; 
                }
            }

            $("#emailInputText").blur(function() {
                emailValid();
            });
            $("#emailInputText").keyup(function() {
                emailValid();
            });
        </script>
    </body>
</html>