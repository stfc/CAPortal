<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html>
<html>


<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>CAOP Home</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for UK CA Operators"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap">
    <div class="container">
        <h1>CAOP home!</h1>
        <p>
            You should only be able to access this page if you have ROLE_CAOP
        </p>

        <div class="container">
            <h2>Tasks</h2>
            [<b>${fn:length(approved_reqrows)}</b>] <b>APPROVED</b>,
            [<b>${fn:length(crr_reqrows)}</b>] <b>REVOKE</b>
            <div class="col-xs-11">
                <table class="table tablecondensed">
                    <thead>
                    <tr>
                        <th>Type</th>
                        <th>Serial</th>
                        <th>Name</th>
                        <th>Submitted On</th>
                        <th>DN</th>
                    </tr>
                    </thead>
                    <c:forEach items="${approved_reqrows}" var="row">
                        <c:url value="/raop/viewcsr?requestId=${row.req_key}" var="viewreq"/>
                        <tr>
                            <td>APPROVED</td>
                            <td><a href="${viewreq}">${row.req_key}</a></td>
                            <td>${row.cn}</td>
                            <td>${row.dataNotBefore}</td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${row.dn}">DN
                                </button>
                            </td>
                        </tr>
                    </c:forEach>

                    <c:forEach items="${crr_reqrows}" var="row">
                        <c:url value="/raop/viewcrr?requestId=${row.crr_key}" var="viewcrr"/>
                        <tr>
                            <td>REVOKE</td>
                            <td><a href="${viewcrr}">${row.crr_key}</a></td>
                            <td>${row.cn}</td>
                            <td>${row.dataSubmit_Date}</td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${row.dn}">DN
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
        </div>
        <div class="container">
            <a href="/caportal/caop/exportcert" class="btn btn-primary btn-xs">Export Certificates</a>
            <a href="/caportal/caop/importcert" class="btn btn-primary btn-xs">Import Certificates</a>
        </div>
        <br>

        Possible functions:
        <ul>
            <li>View/Edit raoplist table and list of RAs</li>
            <li>View/Edit certwiz message of the day</li>
            <li>Promote/Demote certificates</li>
            <li>Extract CSRs from db for signing (upload/download)</li>
            <li>Change the email address associated with (host) certificates (where it isn't part of the DN of course),
                esp where empty.
            </li>
            <li>Multiple levels of CA op role</li>
        </ul>
        <br/>

        <p>
            Dev notes: Your principal object is....: <%= request.getUserPrincipal()%>
        </p>
    </div>

    <%--<jsp:include page="../common/footer.jsp"/>--%>
    <%@ include file="../../jspf/footer.jspf" %>
</body>
</html>