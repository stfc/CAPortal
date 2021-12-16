<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Search CSRs</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for searching CSRs for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/resources/jquery/tablesorter/css/theme.blue.css" rel="stylesheet"/>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>
<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">
            <%--Mydebug[${mydebugmessage}]--%>
            <s:hasBindErrors name="searchCsrFormBean_REQUESTSCOPE">
                <div id="thesemessages" class="error">Invalid GET request search parameter</div>
            </s:hasBindErrors>

            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchcsr"
                       modelAttribute="searchCsrFormBean" cssClass="form-horizontal">
                <div class="form-group">
                    <h2 class="form-search-heading">Search Signing Requests (CSRs)</h2>
                    <br/>
                        <%--<c:if test="${not empty message}">
                            <div id="okmessage" class="success">${message}</div>
                        </c:if>--%>
                    <c:if test="${not empty searchOk}">
                        <div id="message" class="success">${searchOk}</div>
                    </c:if>
                    <s:bind path="*">
                        <c:if test="${status.error}">
                            <div id="errormessage" class="error">Form has errors</div>
                        </c:if>
                    </s:bind>
                    <div class="col-4">
                        <font class="muted">
                            _ matches any single char<br/>
                            % matches a string
                        </font>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>For RA</strong>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="ra" class="form-control">
                            <form:options items="${ralistArray}"/>
                        </form:select>
                        <form:errors path="ra" cssClass="text-error"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>Type</strong>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="status" class="form-control">
                            <form:option value="NEW_or_RENEW"/>
                            <form:option value="NEW"/>
                            <form:option value="RENEW"/>
                            <form:option value="ARCHIVED"/>
                            <form:option value="DELETED"/>
                            <form:option value="APPROVED"/>
                            <form:option value="all"/>
                        </form:select>
                        <form:errors path="status" cssClass="text-error"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>Common Name Like (CN)</strong>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="name" placeholder="A Name" class="form-control"/>
                        <form:errors path="name" cssClass="text-error"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>Distinguished Name Like (DN)</strong>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="dn" placeholder="CN=some body,L=DL,OU=CLRC,O=eScience,C=UK"
                                    class="form-control"/>
                        <form:errors path="dn" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="form-group">
                        <div class="col-3 col-lg-3">
                            <strong>Data Like</strong> <font class="muted">(shown if own ROLE_CAOP)</font>
                        </div>
                        <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                            <form:input path="data" placeholder="CWIZPIN" class="form-control"/>
                            <form:errors path="data" cssClass="text-error"/>
                        </div>
                    </div>
                </sec:authorize>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>Email Address Like</strong>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="emailAddress" class="form-control"
                                    placeholder="someone@world.com"/>
                        <form:errors path="emailAddress" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="form-group">
                        <div class="col-3 col-lg-3">
                            <strong>Email Address is Null</strong>
                        </div>
                        <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                            <form:checkbox path="searchNullEmailAddress"/>
                            <font class="muted">(if checked, this will override email search string above)</font>
                        </div>
                    </div>
                </sec:authorize>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>Serial</strong>
                        <font class="muted" style="text-decoration: underline">
                            (if given, all other search criteria are ignored)
                        </font>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="req_key" placeholder="1234" class="form-control"/>
                        <form:errors path="req_key" cssClass="text-error"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-3 col-lg-3">
                        <strong>Results per page:</strong>
                    </div>
                    <div class="col-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="showRowCount" class="form-control">
                            <form:option value="20"/>
                            <form:option value="50"/>
                            <form:option value="100"/>
                        </form:select>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-offset-3">
                        <button type="submit" class="btn btn-md btn-primary">Search</button>
                    </div>
                </div>
            </form:form>
            <br/>

            <h4>CSR Results (total = ${sessionScope.csrSearchPageHolder.totalRows}
                <c:if test="${sessionScope.lastCSRSearchDate_session != null}">
                    ,&nbsp;${sessionScope.lastCSRSearchDate_session}
                </c:if>)
            </h4>

            <br/>

            <!--<table id="csrResultsTable" class="table table-hover table-condensed">-->
            <div class="col-11">
                <table id="csrResultsTable" class="tablesorter-blue">
                    <!--  <caption>List of certificate rows returned by search</caption> -->
                    <thead>
                    <tr>
                        <th class="sorter-false">#</th>
                        <th>Type</th>
                        <th>Serial</th>
                        <th>Submitted On</th>
                        <th>Email</th>
                        <th>CN</th>
                        <th class="sorter-false">DN</th>
                        <th>bulkID</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:set var="count" value="0" scope="page"/>
                    <c:forEach var="csr" items="${sessionScope.csrSearchPageHolder.source}">
                        <c:url value="/raop/viewcsr?requestId=${csr.req_key}" var="viewcsr"/>
                        <c:url value="/raop/viewbulk?bulkId=${csr.bulk}" var="viewbulk"/>
                        <c:set var="count" value="${count + 1}" scope="page"/>
                        <tr>
                            <td>${sessionScope.csrSearchPageHolder.row + count}</td>
                            <td>${csr.status}</td>
                            <td><a href="${viewcsr}">${csr.req_key}</a></td>
                            <td><fmt:formatDate value="${csr.dataNotBefore}"/></td>
                            <td><a href="mailto:${csr.email}">${csr.email}</a></td>
                            <td>${csr.cn}</td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${csr.dn}">DN
                                </button>
                            </td>
                            <td><a href="${viewbulk}">${csr.bulk}</a></td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

                <table>
                    <tr>
                        <td>
                            <c:if test="${sessionScope.csrSearchPageHolder.totalRows == 0}">
                                Showing:&nbsp;[<b>0</b>]
                            </c:if>
                            <c:if test="${sessionScope.csrSearchPageHolder.totalRows != 0}">
                                Showing:&nbsp;[<b>${sessionScope.csrSearchPageHolder.row+1}</b>] <!--zero offset list -->
                            </c:if>
                            to
                            [<b>${sessionScope.csrSearchPageHolder.row + fn:length(sessionScope.csrSearchPageHolder.source)}</b>]
                            of [<b>${sessionScope.csrSearchPageHolder.totalRows}</b>]
                        </td>
                        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                        <td>Go to row:</td>
                        <td>
                            <form:form method="post" action="${pageContext.request.contextPath}/raop/searchcsr/goto"
                                       modelAttribute="gotoPageFormBean">
                                <form:input path="gotoPageNumber" cssStyle="width:30px" placeholder="0"/>
                                <button type="submit" class="btn btn-sm">Go</button>
                            </form:form>
                        </td>
                        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                        <c:if test="${fn:length(sessionScope.csrSearchPageHolder.source) > 0}">
                        <c:url value="/raop/searchcsr/page?page=next" var="pagenextaction"/>
                        <c:url value="/raop/searchcsr/page?page=prev" var="pageprevaction"/>
                        <c:url value="/raop/searchcsr/page?page=first" var="pagefirstaction"/>
                        <c:url value="/raop/searchcsr/page?page=last" var="pagelastaction"/>
                        <td>
                            <ul class="pager">
                                <li><a href="${pagefirstaction}">First</a></li>
                                <li><a href="${pageprevaction}">&laquo; Previous</a></li>
                                <li><a href="${pagenextaction}">Next &raquo;</a></li>
                                <li><a href="${pagelastaction}">Last</a></li>
                            </ul>
                        </td>
                    </tr>
                    </c:if>
                </table>
            </div>
        </div>
    </div>
</div>


<%--<jsp:include page="../common/footer.jsp" />--%>
<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript"
        src="${pageContext.request.contextPath}/resources/jquery/tablesorter/js/jquery.tablesorter.min.js"></script>
<script>
    $(function () {
        $("#csrResultsTable").tablesorter();
    });
</script>
</body>
</html>
