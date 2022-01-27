
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
    <title>Search Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for searching certificates for RA/CA ops."/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/resources/jquery/tablesorter/css/theme.blue.min.css" rel="stylesheet"/>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<!-- Wrap all page content here -->
<div id="wrap" class="container">
            <s:hasBindErrors name="searchCertFormBean_REQUESTSCOPE">
                <div id="thesemessages" class="error">Invalid GET request search parameter</div>
            </s:hasBindErrors>


            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchcert" modelAttribute="searchCertFormBean" cssClass="form-horizontal">
                <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                <div class="col">
                    <h2 class="form-search-heading">Search for Certificates</h2>
                    <c:if test="${not empty searchOk}">
                        <div id="message" class="success">${searchOk}</div>
                    </c:if>
                    <s:bind path="*">
                        <c:if test="${status.error}">
                            <div id="message" class="error">Form has errors</div>
                        </c:if>
                    </s:bind>
                    <div class="col">
                        <span class="muted">
                            _ matches any single char<br/>
                            % matches a string
                        </span>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Common Name Like (CN)
                    </div>
                    <div class="col">
                        <form:input path="name" class="form-control"
                                    placeholder="A Name"/> <form:errors
                            path="name" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Certificate RA
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
                        Distinguished Name Like (DN)
                    </div>
                    <div class="col">
                        <form:input path="dn" class="form-control"
                                    placeholder="CN=some body,L=DL,OU=CLRC,O=eScience,C=UK"/> <form:errors
                            path="dn" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="row form-cols">
                        <div class="col">
                            Data Like <span class="muted">(shown if own ROLE_CAOP)</span>
                        </div>
                        <div class="col">
                            <form:input path="data" class="form-control"/> <form:errors
                                path="data" cssClass="text-error"/>
                        </div>
                    </div>
                </sec:authorize>
                <div class="row form-cols">
                    <div class="col">
                        Email Address Like
                    </div>
                    <div class="col">
                        <form:input path="emailAddress" class="form-control"
                                    placeholder="someone@world.com"/> <form:errors
                            path="emailAddress" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="row form-cols">
                        <div class="col">
                            Email Address is Null
                        </div>
                        <div class="col">
                            <form:checkbox path="searchNullEmailAddress" />
                            <span class="muted">(if checked,
                            this will override email search string above)</span>
                        </div>
                    </div>
                </sec:authorize>
                <div class="row form-cols">
                    <div class="col">
                        Serial Number <span class="muted" style="text-decoration: underline">(if given,
                        other search criteria are ignored)</span>
                    </div>
                    <div class="col">
                        <form:input path="serial" class="form-control"
                                    placeholder="1234"/> <form:errors
                            path="serial" cssClass="text-error"/>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Role
                    </div>
                    <div class="col">
                        <form:select path="role" class="form-control">
                            <form:option value="all"/>
                            <form:option value="User"/>
                            <form:option value="RA Operator"/>
                            <form:option value="CA Operator"/>
                        </form:select>
                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Status <span class="muted">(note, VALID Certs can be Expired)</span>
                    </div>
                    <div class="col">
                        <form:select path="status" class="form-control">
                            <form:option value="VALID"/>
                            <form:option value="REVOKED"/>
                            <form:option value="SUSPENDED"/>
                            <form:option value="all"/>
                        </form:select>

                    </div>
                </div>
                <div class="row form-cols">
                    <div class="col">
                        Valid Only <span class="muted">(Excludes Expired Certs)</span>
                    </div>
                    <div class="col">
                        <form:checkbox path="notExpired"/>
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

            <h4>Certificate Results (total = ${sessionScope.certSearchPageHolder.totalRows}
                <c:if test="${sessionScope.lastCertSearchDate_session != null}">
                    ,${sessionScope.lastCertSearchDate_session}
                </c:if>)
            </h4>

            <br/>
            <div class="row">
                <table id="certResultsTable" class="tablesorter-blue"><!-- table table-hover table-condensed-->
                    <thead>
                    <tr>
                        <th class="sorter-false">#</th>
                        <th>Serial</th>
                        <th>CN</th>
                        <th>Email</th>
                        <th>Role</th>
                        <th>Status</th>
                        <th>Expiry Date</th>
                        <th class="sorter-false">View DN</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:set var="count" value="0" scope="page"/>
                    <c:forEach var="cert" items="${sessionScope.certSearchPageHolder.source}">
                        <c:url value="/raop/viewcert?certId=${cert.cert_key}" var="viewcert"/>
                        <c:set var="count" value="${count + 1}" scope="page"/>
                        <tr>
                            <td>${sessionScope.certSearchPageHolder.row + count}</td>
                            <td><a href="${viewcert}">${cert.cert_key}</a></td>
                            <td>${cert.cn}
                                <a class="_showDnLink">+dn</a>
                                <div class="_showDnDiv">
                                    <input readonly value="${cert.dn}"/>
                                </div>
                            </td>
                            <td><a href="mailto:${cert.email}">${cert.email}</a></td>
                            <td>${cert.role}</td>
                            <td>${cert.status}</td>
                            <td><fmt:formatDate value="${cert.notAfter}"></fmt:formatDate>
                            </td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-bs-toggle="popover"
                                        data-bs-placement="top" data-bs-content="${cert.dn}">DN
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>


                <table>
                    <tr>
                        <td>
                            <c:if test="${sessionScope.certSearchPageHolder.totalRows == 0}">
                                Showing:[<b>0</b>]
                            </c:if>
                            <c:if test="${sessionScope.certSearchPageHolder.totalRows > 0}">
                                Showing:[<b>${sessionScope.certSearchPageHolder.row+1}</b>]
                            </c:if>
                            to
                            [<b>${sessionScope.certSearchPageHolder.row + fn:length(sessionScope.certSearchPageHolder.source)}</b>]
                            of [<b>${sessionScope.certSearchPageHolder.totalRows}</b>] <!--zero offset list -->
                        </td>

                        <form:form method="post" action="${pageContext.request.contextPath}/raop/searchcert/goto"
                                   modelAttribute="gotoPageFormBean">
                            <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                            <td>Go to row:</td>
                            <td>
                                <form:input path="gotoPageNumber" cssStyle="width:30px" placeholder="0"/>
                                <button type="submit" class="btn btn-secondary">Go</button>
                            </td>
                        </form:form>
                        <c:if test="${fn:length(sessionScope.certSearchPageHolder.source) > 0}">
                            <c:url value="/raop/searchcert/page?page=next" var="pagenextaction"/>
                            <c:url value="/raop/searchcert/page?page=prev" var="pageprevaction"/>
                            <c:url value="/raop/searchcert/page?page=first" var="pagefirstaction"/>
                            <c:url value="/raop/searchcert/page?page=last" var="pagelastaction"/>
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
        </div> <!-- /container -->


<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript"
        src="${pageContext.request.contextPath}/resources/jquery/tablesorter/js/jquery.tablesorter.min.js"></script>

<script>
    $("._showDnDiv").hide();

    $(document).ready(function () {
        $("._showDnLink").click(function () {
            $(this).next("._showDnDiv").toggle();
        });
    });
</script>
<script>
    $(function () {
        $("#certResultsTable").tablesorter();
    });
</script>
</body>
</html>