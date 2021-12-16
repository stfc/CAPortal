<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%--<%@ page session="false"%>--%>
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
    <meta name="description" content="Home page for viewing CRRs for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>
<c:url value="/raop/searchcrr" var="searchcrr"/>

<%--<ul class="breadcrumb">
    <li><a href="${searchcrr}"><<< Search Revocation Request</a> <span class="divider">/</span></li>
</ul>--%>

<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">
            <h2>View CRR - Revocation Request</h2>

            <c:if test="${errorMessage != null}">
                <div id="message" class="error">${errorMessage}</div>
            </c:if>

            <c:if test="${not empty message}">
                <div id="message" class="success">${message}</div>
            </c:if>

            <h4>Page last refreshed: (${lastViewRefreshDate})</h4>
            <div class="col-11 col-lg-10">
                <table class="table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th>Variable</th>
                        <th>Value</th>
                    </tr>
                    </thead>
                    <tbody>
                    <tr>
                        <td>CRR Serial Number</td>
                        <td>${crr.crr_key}</td>
                    </tr>
                    <tr>
                        <td>Submission Date</td>
                        <td>${crr.dataSubmit_Date}</td>
                    </tr>
                    <tr>
                        <td>CN</td>
                        <td>${crr.cn}</td>
                    </tr>
                    <tr>
                        <td>DN</td>
                        <td>${crr.dn}</td>
                    </tr>
                    <tr>
                        <td>Reason</td>
                        <td>${crr.reason}</td>
                    </tr>
                    <tr>
                        <td>Status</td>
                        <td><b><font color="red">${crr.status}</font></b></td>
                    </tr>
                    <tr>
                        <td>Cert's Serial Number</td>
                        <td>
                            <a href="${pageContext.request.contextPath}/raop/viewcert?certId=${crr.cert_key}">${crr.cert_key}</a>
                        </td>
                    </tr>
                    <%--<tr>
                        <td>Data</td>
                        <td><textarea rows="15" style="width: 100%" readonly>${crr.data}</textarea></td>
                    </tr>--%>
                    </tbody>
                </table>
                <c:if test="${canDeleteCrr}">
                    <h3>Actions:</h3>
                    <form:form method="post" action="${pageContext.request.contextPath}/raop/viewcrr/delete"
                               modelAttribute="crr">
                        <form:hidden path="crr_key"/>
                        <button type="submit" class="btn btn-sm"
                                onclick="return confirm('Are you sure you want to delete this certificate revocation request?');">
                            Delete Revocation Request
                        </button>
                        (Certificate status will be reverted to <b>VALID</b> and revocation request status will be <b>DELETED</b>).
                    </form:form>
                </c:if>
                <c:if test="${canApproveCrr}">
                    <form:form method="post" action="${pageContext.request.contextPath}/raop/viewcrr/approve"
                               modelAttribute="crr">
                        <form:hidden path="crr_key"/>
                        <button type="submit" class="btn btn-small"
                                onclick="return confirm('Are you sure you want to approve this certificate revocation request?');">
                            Approve Revocation Request
                        </button>
                        (Certificate status will be <b>SUSPENDED</b> and revocation request will be <b>APPROVED</b>).
                    </form:form>
                </c:if>
            </div>
            <div class="col-11 col-md-9 col-lg-8">
                <h4>Data</h4>
                <textarea rows="15" class="form-control" readonly=>${crr.data}</textarea>
                <br/><br/>
            </div>
        </div>
    </div>


</div>
<%--<jsp:include page="../common/footer.jsp" />--%>
<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>
