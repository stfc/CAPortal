<?xml version="1.0" encoding="ISO-8859-1" ?>
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<!DOCTYPE html PUBLIC "-//W3C//DTD XHTML 1.0 Transitional//EN" "http://www.w3.org/TR/xhtml1/DTD/xhtml1-transitional.dtd">
<html xmlns="http://www.w3.org/1999/xhtml">

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>View RA List</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for viewing the ralist for public."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<%--<jsp:include page="../common/header.jsp" />--%>

<!-- Wrap all page content here -->
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1">
            <div>
                <h2>RA List (${fn:length(editRalistFormBean.source)})</h2>
                <c:if test="${not empty message}">
                    <div id="message" class="success">${message}</div>
                </c:if>
                <c:if test="${status.error}">
                    <div id="message" class="error">Form has errors</div>
                </c:if>
            </div>
            <h4>RAs for your institution are listed below</h4>
            <h5>RA List last refreshed: &nbsp;(${sessionScope.lastRalistSearchDate_session})</h5>
            <h5>Click links to see local RA Operators/contacts</h5>
            <h5>If you are having problems finding an RA, please contact us on the helpdesk and we'll be happy to
                help</h5>
            <br/>
            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchralist/save"
                       modelAttribute="editRalistFormBean" cssClass="form-search">
                <div class="col-xs-11 col-lg-10">
                    <table class="table table-hover table-condensed">
                        <thead>
                        <tr>
                            <th>#</th>
                            <!--<th>ra_id</th>-->
                            <!--<th>order_id</th>-->
                            <th>L (Location)</th>
                            <th>OU (Org Unit)</th>
                            <!--<th>Active</th>--!>
                            <!--<th>RA Operators/Contacts</th>-->
                        </tr>
                        </thead>
                        <!-- the form:checkbox path variable is relative to form:form modelAttribute
                                           variable, so path="source" evaluates to path="editRalistFormBean.source[n].active" -->

                        <tbody>
                        <c:set var="count" value="0" scope="page"/>
                        <c:forEach var="rarow" items="${editRalistFormBean.source}" varStatus="loopVar">
                            <c:set var="count" value="${count + 1}" scope="page"/>
                            <tr>
                                <td>${editRalistFormBean.row + count}</td>
                                    <%--<td>${rarow.ra_id}</td>--%>
                                    <%--<td>${rarow.order_id}</td>--%>
                                <td>
                                    <a href="${pageContext.request.contextPath}/pub/viewRAs?ou=${rarow.ou}">${rarow.ou}</a>
                                </td>
                                <td>${rarow.l}</td>
                                    <%--<td>${rarow.active}</td>--%>
                                    <%--<td><a href="${pageContext.request.contextPath}/pub/viewRAs?ou=${rarow.ou}">RAs</a></td>--%>
                            </tr>
                        </c:forEach>
                        </tbody>
                    </table>
                </div>
            </form:form>


        </div> <!-- /container -->
    </div>
</div> <!-- /span -->


<%--<jsp:include page="../common/footer.jsp" />--%>
<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>