<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>RAOP Home</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for UK RA operators"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">

            <h2>Pending Requests for Your RA [<span class="text-info">${ra}</span>]</h2>
            <h4>(RAs, please bookmark this page to view your pending requests)</h4>
            <br/>
            [<b>${fn:length(new_reqrows)}</b>] <b>NEW</b>,
            [<b>${fn:length(renew_reqrows)}</b>] <b>RENEW</b>,
            [<b>${fn:length(crr_reqrows)}</b>] <b>REVOKE</b>
            &nbsp;&nbsp;&nbsp;&nbsp;Last Refreshed: <b>${lastPageRefreshDate}</b>
            <br/>
            <br/>
            <div class="col-11">
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
                    <c:forEach items="${new_reqrows}" var="row">
                        <c:url value="/raop/viewcsr?requestId=${row.req_key}" var="viewreq"/>
                        <tr>
                            <td>NEW</td>
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

                    <c:forEach items="${renew_reqrows}" var="row">
                        <c:url value="/raop/viewcsr?requestId=${row.req_key}" var="viewrenewreq"/>
                        <tr>
                            <td>RENEW</td>
                            <td><a href="${viewrenewreq}">${row.req_key}</a></td>
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
    </div>
</div>
<%--<jsp:include page="../common/footer.jsp"/>--%>
<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>