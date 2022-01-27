<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<!doctype html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>View Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for viewing certificates for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>
<c:url value="/raop/viewcert" var="faction"/>
<c:url value="/raop" var="raophome"/>
<c:url value="/raop/searchcert" var="searchcert"/>


<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <h2>View Certificate</h2>
        <c:if test="${errorMessage != null}">
            <span id="message" class="error">${errorMessage}</span>
        </c:if>
        <c:if test="${not empty message}">
            <span id="message" class="success">${message}</span>
        </c:if>
        <c:if test="${not empty emailUpdateOkMessage}">
            <span id="message" class="success">${emailUpdateOkMessage}</span>
        </c:if>
        <c:if test="${not empty emailUpdateFailMessage}">
            <span id="message" class="error">${emailUpdateFailMessage}</span>
        </c:if>
        <c:if test="${not empty roleChangeOkMessage}">
            <span id="message" class="success">${roleChangeOkMessage}</span>
        </c:if>
        <c:if test="${not empty roleChangeFailMessage}">
            <span id="message" class="error">${roleChangeFailMessage}</span>
        </c:if>
        <h4>Last Page Refresh: (${lastViewRefreshDate})</h4>
        <br/>
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
                <td>${cert.cert_key}, (hex: ${hexSerial})</td>
            </tr>
            <tr>
                <td>Common Name (CN)</td>
                <c:url value="/raop/searchcsr/search?name=${cert.cn}&ra=all&status=all&searchNullEmailAddress=false"
                       var="backsearchcsroncn"/>
                <td><a href="${backsearchcsroncn}">${cert.cn}</a> &larr; <span class="muted">search for matching CSRs for CN</span>
                </td>
            </tr>
            <tr>
                <td>Distinguished Name (DN)</td>
                <td>${cert.dn}</td>
            </tr>
            <tr>
                <td>Issuer DN</td>
                <td>${certObj.issuerDN}</td>
            </tr>
            <tr>
                <td>SANs (if any)</td>
                <td><c:forEach var="san" items="${sans}">${san} </c:forEach></td>
            </tr>
            <tr>
                <td>Email</td>
                <td><a href="mailto:${cert.email}" id="currEmail">${cert.email}</a>
                    <form:form method="post"
                               action="${pageContext.request.contextPath}/raop/viewcert/rachangemail">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <div id="inputEmail">
                            <input id="emailInputText" name="email" value="${cert.email}"/><span></span>
                        </div>
                        <input name="cert_key" type="hidden" value="${cert.cert_key}"/>
                        <c:if test="${canEditEmail}">
                            <button type="button" id="changeEmail" class="btn btn-primary btn-xs">Change host
                                email
                            </button>
                            <button type="submit" id="saveEmail" class="btn btn-primary btn-xs"
                                    onclick="return confirm('Are you sure you want to change the host email address?')">
                                Save
                            </button>
                            <a onclick="location.reload(true);">Cancel</a>
                        </c:if>
                    </form:form>
                </td>
            </tr>
            <tr>
                <td>Status</td>
                <td><b>
                    <c:if test="${cert.status == 'VALID'}">
                        <span class="text-success">${cert.status}</span>
                    </c:if>
                    <c:if test="${cert.status != 'VALID'}">
                        <span class="text-danger">${cert.status}</span>
                    </c:if>
                </b></td>
            </tr>
            <tr>
                <td>Role</td>
                <td>${cert.role}</td>
            </tr>
            <tr>
                <td>Pin</td>
                <td>${pin}</td>
            </tr>
            <tr>
                <td>Last Action Date</td>
                <td>${lastActionDate}</td>
            </tr>
            <tr>
                <td>Profile</td>
                <td>${profile}</td>
            </tr>
            <tr>
                <td>Not Before (Starts)</td>
                <td>${certObj.notBefore}</td>
            </tr>
            <tr>
                <td>Not After (Expires)</td>
                <td>${certObj.notAfter}</td>
            </tr>

            <tr>
                <td>Signature Algorithm</td>
                <td>${certObj.sigAlgName}</td>
            </tr>
            <tr>
                <td>Type</td>
                <td>${certObj.type}</td>
            </tr>
            <tr>
                <td>Version</td>
                <td>${certObj.version}</td>
            </tr>

            </tbody>
        </table>
    </div>

    <div class="row">
        <h3>Actions</h3>
        <!-- Always show any form revoke error messages out of the
        conditional view (if) statements below -->
        <c:if test="${formRevokeErrorMessage != null}">
            <div id="message" class="error">${formRevokeErrorMessage}</div>
        </c:if>

        <!-- Not all certs can be revoked  -->
        <c:if test="${canRevokeCert}">
        <!-- Only home RA or CAOP can do full revoke -->
        <c:if test="${viewerCanFullRevoke}">
            <div class="row form-cols">
                <form:form method="post" action="${pageContext.request.contextPath}/raop/viewcert/fullrevoke"
                           modelAttribute="revokeCertFormBean">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input name="cert_key" type="hidden" value="${cert.cert_key}"/>
                    <div class="col">
                        <form:input path="reason" class="form-control"
                                    placeholder="Reason to revoke (value is required)"/>
                    </div>
                    <div class="col">
                        <button type="submit" class="btn btn-sm btn-danger"
                                onclick="return confirm('Are you sure you want to approve a full revocation of this certificate?');">
                            Revoke Certificate
                        </button>
                    </div>
                </form:form>
            </div>
            <div class="row">
                <p>Certificate will be <b>SUSPENDED</b> and an <b>APPROVED</b> revocation request will be created</p>
            </div>
            <br/>
        </c:if>

        <!-- Any RA/CAOP can request a revoke -->
        <c:if test="${!viewerCanFullRevoke}">
            <div class="row form-cols">
                <form:form method="post"
                           action="${pageContext.request.contextPath}/raop/viewcert/requestrevoke"
                           modelAttribute="revokeCertFormBean">
                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                    <input name="cert_key" type="hidden" value="${cert.cert_key}"/>
                    <div class="col">
                        <form:input path="reason" class="form-control"
                                    placeholder="Reason to revoke (value is required)"/>
                    </div>
                    <div class="col">
                        <button type="submit" class="btn btn-sm btn-primary"
                                onclick="return confirm('Are you sure you want to request revocation of this certificate?');">
                            Request Revocation
                        </button>
                    </div>
                </form:form>
            </div>
            <div class="row">
                <p>Certificate will be <b>SUSPENDED</b> and a <b>NEW</b> revocation request will be created</p>
            </div>
            <br />
        </c:if>
    </div>
    </c:if>
    <div class="row">
        <!-- Ability to Promote/Demote Certificates -->
        <c:if test="${viewerCanPromoteDemote}">
            <h3>Promote/Demote</h3><br/>
            <p>Current Role: ${cert.role}</p>
            <form:form method="post"
                       action="${pageContext.request.contextPath}/raop/viewcert/changerole">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <input name="cert_key" type="hidden" value="${cert.cert_key}"/>
                <div class="row form-cols">
                    <div class="col">
                        <select name="operation" style="width: 150px; height: 35px">
                            <c:if test="${canPromote}">
                                <option selected value="promote">
                                    Promote
                                </option>
                            </c:if>
                            <c:if test="${canDemote}">
                                <option value="demote">
                                    Demote
                                </option>
                            </c:if>
                        </select>
                    </div>

                    <div class="col">
                        <button type="submit" class="btn btn-sm btn-primary"
                                onclick="return confirm('Are you sure you want to perform this application?');">
                            Apply
                        </button>
                    </div>
                </div>
            </form:form>
        </c:if>
        <h4>Data</h4>
        <textarea rows="15" class="form-control" readonly="readonly">${cert.data}</textarea>
    </div>
</div>

<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript">
    function emailValid() {
        const inputEmail = $("#emailInputText");
        const currEmail = $("#currEmail").text();
        //adds error text and disables save button if not an email address or email same as current email
        if (inputEmail.val().match(/^[_A-Za-z0-9-\+]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9]+)*(\.[A-Za-z]{2,})$/) === null) {
            $(inputEmail).addClass("error").next().text("Invalid email");
            //disable save button
            document.getElementById("saveEmail").disabled = true;
            return false;
        } else if (inputEmail.val() === currEmail) {
            $(inputEmail).addClass("error").next().text("No email change");
            //disable save button
            document.getElementById("saveEmail").disabled = true;
            return false;
        } else {
            $(inputEmail).removeClass("error").next().text("");
            //re-enable save button
            document.getElementById("saveEmail").disabled = false;
            return true;
        }
    }

    $("#emailInputText").blur(function () {
        emailValid();
    });
    $("#emailInputText").keyup(function () {
        emailValid();
    });
</script>

</body>
</html>
