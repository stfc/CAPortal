<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%@page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<!DOCTYPE html>

<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Revoke Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Revoke certificate page for UK CA certificate owners"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body>
<%--<jsp:include page="../common/header.jsp"/>--%>
<%@ include file="../../jspf/header.jspf" %>
<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">
            <h2>Revoke Certificate</h2>
            <c:if test="${not empty revokeOkMessage}">
                <div class="success">${revokeOkMessage}</div>
            </c:if>
            <div class="col-11 col-lg-10">
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
                        <td>Common Name (CN)</td>
                        <td>${certificateRow.cn}</td>
                    </tr>
                    <tr>
                        <td>Distinguished Name (DN)</td>
                        <td>${cert.subjectDN}</td>
                    </tr>
                    <tr>
                        <td>Issuer DN</td>
                        <td>${cert.issuerDN}</td>
                    </tr>
                    <tr>
                        <td>Email</td>
                        <td><a href="mailto:${certificateRow.email}">${certificateRow.email}</a></td>
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
                        <td>Not Before</td>
                        <td>${cert.notBefore}</td>
                    </tr>
                    <tr>
                        <td>Not After</td>
                        <td>${cert.notAfter}</td>
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
            </div>

            <div class="form-group">

                <c:if test="${certificateRow.status == 'VALID'}">
                    <form:form method="post"
                               action="${pageContext.request.contextPath}/cert_owner/revoke"
                               modelAttribute="revokeCertFormBean">
                        <div class="col-8 col-lg-5">

                            <input name="cert_key" type="hidden"
                                   value="${certificateRow.cert_key}"/>
                            <form:input path="reason" class="form-control"
                                        placeholder="Reason to revoke (value is required)"/>
                            <br/>
                            <form:errors path="reason" cssClass="text-error"/>
                        </div>
                        <div class="col-1 col-lg-1">
                            <button type="submit" class="btn btn-sm btn-primary"
                                    onclick="return confirm('Are you sure you want to approve a full revocation of this certificate?');">
                                Revoke Certificate
                            </button>
                        </div>
                    </form:form>
                </c:if>
            </div>


            <br/><br/>

            <%-- <p>
            Your principal object is....: <%= request.getUserPrincipal() %>
            </p> --%>
        </div>
    </div>
</div>
<%@ include file="../../jspf/footer.jspf" %>

</body>
</html>