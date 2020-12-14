<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>View Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for viewing CSRs for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <meta name="author" content="Sam Worley"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>
<%--
<c:url value="/raop/searchcsr" var="searchcsr"/>
<ul class="breadcrumb">
    <li><a href="${searchcsr}"><<< Search Signing Request</a> <span class="divider">/</span></li>
</ul>
--%>

<!-- Wrap all page content here -->
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1">
            <div class="col-xs-11 col-lg-10">
                <h2>View CSR - Signing Request</h2>

                <c:if test="${errorMessage != null}">
                    <div id="message" class="error">${errorMessage}</div>
                </c:if>

                <c:if test="${not empty message}">
                    <div id="message" class="success">${message}</div>
                </c:if>

                <h4>(${lastViewRefreshDate})</h4>
                <table class="table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th>Request Details</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>Serial</td>
                        <td>${csr.req_key}</td>
                    </tr>
                    <tr>
                        <td>CN</td>
                        <c:url value="/raop/searchcert/search?name=${csr.cn}&ra=all&status=VALID&notExpired=true&roll=all&searchNullEmailAddress=false"
                               var="backsearchcert"/>
                        <td><a href="${backsearchcert}">${csr.cn}</a> &larr; <font class="muted">search for matching
                            certificates</font></td>
                    </tr>
                    <tr>
                        <td>DN</td>
                        <%--<c:url value="/raop/searchcert/search?dn=${csr.dn}" var="backsearchcertdn"/>--%>
                        <%--<td><a href="${backsearchcertdn}">${csr.dn}</a> &larr; <font class="muted">search for matching certificates</font></td>--%>
                        <td>${csr.dn}</td>
                    </tr>
                    <tr>
                        <td>&nbsp;&nbsp;&nbsp;VALID/Non-Expired Certs with same DN:</td>
                        <td>
                            <c:forEach var="_certID" items="${validNotExpiredCertIdsWithDN}">
                                <a href="${pageContext.request.contextPath}/raop/viewcert?certId=${_certID}">${_certID}</a>
                            </c:forEach>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;&nbsp;&nbsp;REVOKED Certs with same DN:</td>
                        <td>
                            <c:forEach var="_certID" items="${revokedCertIdsWithDN}">
                                <a href="${pageContext.request.contextPath}/raop/viewcert?certId=${_certID}">${_certID}</a>
                            </c:forEach>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;&nbsp;&nbsp;Expired Certs with same DN:</td>
                        <td>
                            <c:forEach var="_certID" items="${validExpiredCertIdsWithDN}">
                                <a href="${pageContext.request.contextPath}/raop/viewcert?certId=${_certID}">${_certID}</a>
                            </c:forEach>
                        </td>
                    </tr>
                    <tr>
                        <td>&nbsp;&nbsp;&nbsp;OTHER Certs with same DN:</td>
                        <td>
                            <c:forEach var="_certID" items="${otherCertIdsWithDN}">
                                <a href="${pageContext.request.contextPath}/raop/viewcert?certId=${_certID}">${_certID}</a>
                            </c:forEach>
                        </td>
                    </tr>
                    <tr>
                        <td>Email</td>
                        <td>${csr.email}</td>
                    </tr>
                    <tr>
                        <td>RA</td>
                        <td>${csr.ra}</td>
                    </tr>
                    <tr>
                        <td>RAO</td>
                        <td>${csr.rao}</td>
                    </tr>
                    <tr>
                        <td>Status</td>
                        <td><b><font color="red">${csr.status}</font></b></td>
                    </tr>
                    <tr>
                        <td>Role</td>
                        <td>${csr.role}</td>
                    </tr>
                    <tr>
                        <td>Submitted By</td>
                        <c:url value="/raop/viewcert?certId=${ownerserial}" var="backsearchcert2"/>
                        <td><a href="${backsearchcert2}">${ownerserial}</a>&nbsp;&nbsp;${ownerdn}</td>
                    </tr>
                    <tr>
                        <td>Submitted On</td>
                        <td>${notbefore}</td>
                    </tr>
                    <tr>
                        <td>Pin</td>
                        <td>
                            ${pin}&nbsp;&nbsp;
                            <%--<a href="${pageContext.request.contextPath}/raop/verifyPin?pin=${pin}">Verify (opens in new window)</a>--%>
                            <a href="JavaScript:newPopup('${pageContext.request.contextPath}/raop/verifyPin?pin=${pin}');">Verify
                                Pin (opens in new window)</a>
                        </td>
                    </tr>
                    </tbody>
                </table>
                <br/><br/>
                <table class="table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th>Is Part of Bulk Submission</th>
                        <th></th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>Is Bulk</td>
                        <td>
                            <c:choose>
                                <c:when test="${not empty csr.bulk}">
                                    <span class="glyphicon glyphicon-ok"
                                          style="color:green; font-size: large;"></span> &nbsp; <br/>
                                    <button type="button" class="btn btn-xs btn-primary"
                                            onclick="window.location='${pageContext.request.contextPath}/raop/viewbulk?bulkId=${csr.bulk}';">
                                        Manage Bulk
                                    </button>
                                    (Approve/Delete selected CSRs in Bulk)
                                    <br/><br/>
                                    bulkId=<b>${csr.bulk}</b> also contains:<br/>
                                    <c:forEach var="_csrID" items="${csr_serials_cns_with_same_bulkid}">
                                        <c:if test="${csr.req_key ne _csrID.key}">
                                            <a href="${pageContext.request.contextPath}/raop/viewcsr?requestId=${_csrID.key}">${_csrID.key}</a> (CN=${_csrID.value})
                                            <br/>
                                        </c:if>
                                    </c:forEach>
                                </c:when>
                                <c:otherwise>
                                    No
                                </c:otherwise>
                            </c:choose>
                        </td>
                    </tr>
                    </tbody>
                </table>

                <table>
                    <thead>
                    <tr>
                        <th>CSR Actions</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <c:if test="${csr.status == 'NEW' || csr.status =='RENEW'}">
                            <td>
                                <form:form method="post"
                                           action="${pageContext.request.contextPath}/raop/viewcsr/approve"
                                           commandName="csr">
                                    <form:hidden path="status"/>
                                    <form:hidden path="req_key"/>
                                    <button type="submit" class="btn btn-sm btn-primary"
                                            onclick="return confirm('Are you sure you want to Approve this request?');">
                                        Approve Request
                                    </button>
                                    &nbsp;&nbsp;
                                </form:form>
                            </td>
                        </c:if>
                        <c:if test="${csr.status == 'NEW' || csr.status =='RENEW' || csr.status == 'APPROVED'}">
                            <td>
                                <form:form method="post" action="${pageContext.request.contextPath}/raop/viewcsr/delete"
                                           commandName="csr">
                                    <form:hidden path="status"/>
                                    <form:hidden path="req_key"/>
                                    <button type="submit" class="btn btn-sm btn-danger"
                                            onclick="return confirm('Are you sure you want to Delete this request?');">
                                        Delete Request
                                    </button>
                                </form:form>
                            </td>
                        </c:if>
                    </tr>
                    </tbody>
                </table>
            </div>

            <div class="col-xs-11 col-md-9 col-lg-8">
                <br/><br/>
                <h4>Data</h4>
                <textarea rows="15" class="form-control" readonly="readonly">${csr.data}</textarea>
                <br/><br/>
            </div>
        </div>
    </div>
</div>


<%--<jsp:include page="../common/footer.jsp" />--%>
<%@ include file="../../jspf/footer.jspf" %>
<script>
    function confirmApprove() {
        var r = confirm("Are you sure you want to Approve this request?");
        return r;
    }
</script>
<script type="text/javascript">
    function newPopup(url) {
        popupWindow = window.open(
            url, 'popUpWindow',
            'height=400,width=700,left=10,top=10,resizable=yes,scrollbars=yes,toolbar=yes,menubar=no,location=no,directories=no,status=yes');
    }
</script>

</body>
</html>
