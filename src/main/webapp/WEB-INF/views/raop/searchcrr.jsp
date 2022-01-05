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
    <title>Search CRRs</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for searching CRRs for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/resources/jquery/tablesorter/css/theme.blue.min.css" rel="stylesheet"/>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>
<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <div class="col">

            <s:hasBindErrors name="searchCrrFormBean_REQUESTSCOPE">
                <div id="thesemessages" class="error">Invalid GET request search parameter</div>
            </s:hasBindErrors>

            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchcrr"
                       modelAttribute="searchCrrFormBean" cssClass="form-horizontal">
                <div class="form-group">
                    <h2 class="form-search-heading">Search Revocation Requests (CRRs)</h2>
                    <br/>
                        <%--<c:if test="${not empty message}">
                            <div id="message" class="success">${message}</div>
                        </c:if>--%>
                    <c:if test="${not empty searchOk}">
                        <div id="message" class="success">${searchOk}</div>
                    </c:if>
                    <s:bind path="*">
                        <c:if test="${status.error}">
                            <div id="message" class="error">Form has errors</div>
                        </c:if>
                    </s:bind>
                    <div class="col-4">
                        <font class="muted">
                            _ matches any single char<br/>
                            % matches a string
                        </font>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        <strong>For RA</strong>
                    </div>
                    <div class="col">
                        <form:select path="ra" class="form-control">
                            <form:options items="${ralistArray}"/>
                        </form:select>
                        <form:errors path="ra" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        <strong>Type</strong>
                    </div>
                    <div class="col">
                        <form:select path="status" class="form-control">
                            <form:option value="NEW"/>
                            <form:option value="ARCHIVED"/>
                            <form:option value="DELETED"/>
                            <form:option value="APPROVED"/>
                            <form:option value="all"/>
                        </form:select>
                        <form:errors path="status" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        <strong>Common Name Like (CN)</strong>
                    </div>
                    <div class="col">
                        <form:input path="name" placeholder="A Name" class="form-control"/>
                        <form:errors path="name" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        <strong>Distinguished Name Like (DN)</strong>
                    </div>
                    <div class="col">
                        <form:input path="dn" placeholder="CN=some body,L=DL,OU=CLRC,O=eScience,C=UK"
                                    class="form-control"/>
                        <form:errors path="dn" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="row form-cols">
                        <div class="col">
                            <strong>Data Like</strong> <font class="muted">(shown if own ROLE_CAOP)</font>
                        </div>
                        <div class="col">
                            <form:input path="data" placeholder="CWIZPIN" class="form-control"/>
                            <form:errors path="data" cssClass="text-error"/>
                        </div>
                    </div>
                </sec:authorize>
                <div class="row form-cols">
                    <div class="col">
                        <strong>Serial</strong> <font class="muted" style="text-decoration: underline">
                        (if given, all other
                        search criteria are ignored)</font>
                    </div>
                    <div class="col">
                        <form:input path="crr_key" placeholder="1234" class="form-control"/>
                        <form:errors path="crr_key" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        <strong>Results per page:</strong>
                    </div>
                    <div class="col">
                        <form:select path="showRowCount" class="form-control">
                            <form:option value="20"/>
                            <form:option value="50"/>
                            <form:option value="100"/>
                        </form:select>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        <button type="submit" class="btn btn-md btn-primary">Search</button>
                    </div>
                </div>
            </form:form>

            <br/>

            <h4>CRR Search Results (total = ${sessionScope.crrSearchPageHolder.totalRows}
                <c:if test="${sessionScope.lastCRRSearchDate_session != null}">
                    ,&nbsp;${sessionScope.lastCRRSearchDate_session}
                </c:if>)
            </h4>
            <br/>
            <div class="col-11">
                <table id="csrResultsTable" class="tablesorter-blue"> <!--class="table table-hover table-condensed"-->
                    <!--  <caption>List of certificate rows returned by search</caption> -->
                    <thead>
                    <tr>
                        <th class="sorter-false">#</th>
                        <th>Type</th>
                        <th>Serial</th>
                        <th>Submitted On</th>
                        <th>Revoke CN</th>
                        <th class="sorter-false">Revoke DN</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:set var="count" value="0" scope="page"/>
                    <c:forEach var="crr" items="${sessionScope.crrSearchPageHolder.source}">
                        <c:url value="/raop/viewcrr?requestId=${crr.crr_key}" var="viewcrr"/>
                        <c:set var="count" value="${count + 1}" scope="page"/>
                        <tr>
                            <td>${sessionScope.crrSearchPageHolder.row + count}</td>
                            <td>${crr.status}</td>
                            <td><a href="${viewcrr}">${crr.crr_key}</a></td>
                                <%--<td><a href="mailto:${crr.email}">${crr.email}</td>--%>
                            <td><fmt:formatDate value="${crr.dataSubmit_Date}"/></td>
                                <%--<td>${crr.dataSubmit_Date}</td>--%>
                            <td>${crr.cn}</td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${crr.dn}">DN
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

                <table>
                    <tr>
                        <td>
                            <c:if test="${sessionScope.crrSearchPageHolder.totalRows == 0}">
                                Showing:&nbsp;[<b>0</b>] <!--zero offset list -->
                            </c:if>
                            <c:if test="${sessionScope.crrSearchPageHolder.totalRows != 0}">
                                Showing:&nbsp;[<b>${sessionScope.crrSearchPageHolder.row+1}</b>] <!--zero offset list -->
                            </c:if>
                            to
                            [<b>${sessionScope.crrSearchPageHolder.row + fn:length(sessionScope.crrSearchPageHolder.source)}</b>]
                            of [<b>${sessionScope.crrSearchPageHolder.totalRows}</b>]
                        </td>
                        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                        <form:form method="post" action="${pageContext.request.contextPath}/raop/searchcrr/goto"
                                   modelAttribute="gotoPageFormBean">
                            <td>Go to row:</td>
                            <td>
                                <form:input path="gotoPageNumber" cssStyle="width:30px" placeholder="0"/>
                                <button type="submit" class="btn btn-light">Go</button>
                            </td>
                        </form:form>
                        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                        <c:if test="${fn:length(sessionScope.crrSearchPageHolder.source) > 0}">
                            <c:url value="/raop/searchcrr/page?page=next" var="pagenextaction"/>
                            <c:url value="/raop/searchcrr/page?page=prev" var="pageprevaction"/>
                            <c:url value="/raop/searchcrr/page?page=first" var="pagefirstaction"/>
                            <c:url value="/raop/searchcrr/page?page=last" var="pagelastaction"/>
                            <td>
                                <ul class="pagination">
                                    <li class="page-item"><a class="page-link" href="${pagefirstaction}">First</a></li>
                                    <li class="page-item"><a class="page-link" href="${pageprevaction}">&laquo; Previous</a></li>
                                    <li class="page-item"><a class="page-link" href="${pagenextaction}">Next &raquo;</a></li>
                                    <li class="page-item"><a class="page-link" href="${pagelastaction}">Last</a></li>
                                </ul>
                            </td>
                        </c:if>
                    </tr>
                </table>
            </div>
        </div>
    </div>
</div>

<%--<jsp:include page="../common/footer.jsp" />--%>
<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript"
        src="${pageContext.request.contextPath}/resources/jquery/tablesorter/js/jquery.tablesorter.min.js"></script>
<%--<script type="text/javascript" src="${pageContext.request.contextPath}/resources/jquery/tablesorter/js/jquery.tablesorter.widgets.min.js"></script>--%>

<script>
    $(function () {
        $("#csrResultsTable").tablesorter();
    });
</script>
</body>
</html>
