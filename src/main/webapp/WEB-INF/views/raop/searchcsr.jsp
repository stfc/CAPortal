
<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>

<!doctype html>
<html>

<head>

    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Search CSRs</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for searching CSRs for RA/CA ops."/>
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
            <%--Mydebug[${mydebugmessage}]--%>
            <s:hasBindErrors name="searchCsrFormBean_REQUESTSCOPE">
                <div id="thesemessages" class="error">Invalid GET request search parameter</div>
            </s:hasBindErrors>

            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchcsr"
                       modelAttribute="searchCsrFormBean" cssClass="form-horizontal">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="row form-cols">
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
                        <span class="muted">
                            _ matches any single char<br/>
                            % matches a string
                        </span>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        For RA
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
                        Type
                    </div>
                    <div class="col">
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
                <div class="row form-cols">
                    <div class="col">
                        Common Name Like (CN)
                    </div>
                    <div class="col">
                        <form:input path="name" placeholder="A Name" class="form-control"/>
                        <form:errors path="name" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Distinguished Name Like (DN)
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
                            Data Like <span class="muted">(shown if own ROLE_CAOP)</span>
                        </div>
                        <div class="col">
                            <form:input path="data" placeholder="CWIZPIN" class="form-control"/>
                            <form:errors path="data" cssClass="text-error"/>
                        </div>
                    </div>
                </sec:authorize>
                <div class="row form-cols">
                    <div class="col">
                        Email Address Like
                    </div>
                    <div class="col">
                        <form:input path="emailAddress" class="form-control"
                                    placeholder="someone@world.com"/>
                        <form:errors path="emailAddress" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="row form-cols">
                        <div class="col">
                            Email Address is Null
                        </div>
                        <div class="col">
                            <form:checkbox path="searchNullEmailAddress"/>
                            <span class="muted">(if checked, this will override email search string above)</span>
                        </div>
                    </div>
                </sec:authorize>
                <div class="row form-cols">
                    <div class="col">
                        Serial
                        <span class="muted" style="text-decoration: underline">
                            (if given, all other search criteria are ignored)
                        </span>
                    </div>
                    <div class="col">
                        <form:input path="req_key" placeholder="1234" class="form-control"/>
                        <form:errors path="req_key" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Results per page:
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

            <h4>CSR Results (total = ${sessionScope.csrSearchPageHolder.totalRows}
                <c:if test="${sessionScope.lastCSRSearchDate_session != null}">
                    ,${sessionScope.lastCSRSearchDate_session}
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
                                        data-bs-toggle="popover"
                                        data-bs-placement="top" data-bs-content="${csr.dn}">DN
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
                                Showing:[<b>0</b>]
                            </c:if>
                            <c:if test="${sessionScope.csrSearchPageHolder.totalRows != 0}">
                                Showing:[<b>${sessionScope.csrSearchPageHolder.row+1}</b>] <!--zero offset list -->
                            </c:if>
                            to
                            [<b>${sessionScope.csrSearchPageHolder.row + fn:length(sessionScope.csrSearchPageHolder.source)}</b>]
                            of [<b>${sessionScope.csrSearchPageHolder.totalRows}</b>]
                        </td>

                        <td>Go to row:</td>
                        <td>
                            <form:form method="post" action="${pageContext.request.contextPath}/raop/searchcsr/goto"
                                       modelAttribute="gotoPageFormBean">
                                <form:input path="gotoPageNumber" cssStyle="width:30px" placeholder="0"/>
                                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                                <button type="submit" class="btn btn-secondary">Go</button>
                            </form:form>
                        </td>

                        <c:if test="${fn:length(sessionScope.csrSearchPageHolder.source) > 0}">
                        <c:url value="/raop/searchcsr/page?page=next" var="pagenextaction"/>
                        <c:url value="/raop/searchcsr/page?page=prev" var="pageprevaction"/>
                        <c:url value="/raop/searchcsr/page?page=first" var="pagefirstaction"/>
                        <c:url value="/raop/searchcsr/page?page=last" var="pagelastaction"/>
                        <td>
                            <ul class="pagination">
                                <li class="page-item"><a class="page-link" href="${pagefirstaction}">First</a></li>
                                <li class="page-item"><a class="page-link" href="${pageprevaction}">&laquo; Previous</a></li>
                                <li class="page-item"><a class="page-link" href="${pagenextaction}">Next &raquo;</a></li>
                                <li class="page-item"><a class="page-link" href="${pagelastaction}">Last</a></li>
                            </ul>
                        </td>
                    </tr>
                    </c:if>
                </table>
            </div>
        </div>
    </div>
</div>



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
