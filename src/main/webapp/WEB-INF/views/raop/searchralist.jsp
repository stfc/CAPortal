
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<!doctype html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Search RA List</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for searching the ralist for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>

<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">

            <div>
                <h2 class="form-search-heading">RA List (${fn:length(editRalistFormBean.source)})</h2>
                <c:if test="${not empty message}">
                    <div id="message" class="success">${message}</div>
                </c:if>
                <c:if test="${status.error}">
                    <div id="message" class="error">Form has errors</div>
                </c:if>
            </div>

            <br/>
            <sec:authorize access="hasRole('ROLE_CAOP')">
                You have ROLE_CAOP so you can edit the table below by clicking Submit Changes.
            </sec:authorize>
            <sec:authorize access="!hasRole('ROLE_CAOP')">
                ROLE_RAOP can only view the RA list.
            </sec:authorize>

            <br/>

            <h4>Search Results (${sessionScope.lastRalistSearchDate_session})</h4>


            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchralist/save"
                       modelAttribute="editRalistFormBean" cssClass="form-search">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>

                <table class="table table-hover table-condensed">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>ra_id</th>
                        <th>order_id</th>
                        <th>L (Location)</th>
                        <th>OU (Org Unit)</th>
                        <th>Active</th>
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
                            <td>${rarow.ra_id}</td>
                            <td>${rarow.order_id}</td>
                            <td>${rarow.ou}</td>
                            <td>${rarow.l}</td>
                            <td>
                                <sec:authorize access="hasRole('ROLE_CAOP')">
                                    <form:checkbox path="source[${loopVar.index}].active"/>
                                </sec:authorize>
                                <sec:authorize access="!hasRole('ROLE_CAOP')">
                                    ${rarow.active}
                                </sec:authorize>

                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <button type="submit" class="btn btn-light btn-primary">Submit Changes</button>
                </sec:authorize>

            </form:form>


        </div> <!-- /container -->
    </div>
</div> <!-- /span -->



<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>