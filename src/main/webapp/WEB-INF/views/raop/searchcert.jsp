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
    <title>Search Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Home page for searching certificates for RA/CA ops."/>
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
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1">

            <%--<c:if test="${status.error}">
                <div id="messagess" class="error">Form has errors</div>
            </c:if>--%>
            <%--<c:if test="${not empty requestScope['org.springframework.validation.BindingResult.searchCertFormBean_REQUESTSCOPE'].allErrors}">
                An Error has occurred!!!
            </c:if>--%>
            <s:hasBindErrors name="searchCertFormBean_REQUESTSCOPE">
                <div id="thesemessages" class="error">Invalid GET request search parameter</div>
            </s:hasBindErrors>


            <form:form id="form" method="post" action="${pageContext.request.contextPath}/raop/searchcert"
                       modelAttribute="searchCertFormBean" cssClass="form-horizontal">
                <div class="form-group">
                    <h2 class="form-search-heading">Search for Certificates</h2>
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
                    <div class="col-xs-4">
                        <font class="muted">
                            _ matches any single char<br/>
                            % matches a string
                        </font>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Common Name Like (CN)</strong>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="name" class="form-control"
                                    placeholder="A Name"/> <form:errors
                            path="name" cssClass="text-error"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Certificate RA</strong>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="ra" class="form-control">
                            <form:options items="${ralistArray}"/>
                        </form:select>
                        <form:errors path="ra" cssClass="text-error"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Distinguished Name Like (DN)</strong>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="dn" class="form-control"
                                    placeholder="CN=some body,L=DL,OU=CLRC,O=eScience,C=UK"/> <form:errors
                            path="dn" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="form-group">
                        <div class="col-xs-3 col-lg-3">
                            <strong>Data Like</strong> <font class="muted">(shown if own ROLE_CAOP)</font>
                        </div>
                        <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                            <form:input path="data" class="form-control"/> <form:errors
                                path="data" cssClass="text-error"/>
                        </div>
                    </div>
                </sec:authorize>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Email Address Like</strong>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="emailAddress" class="form-control"
                                    placeholder="someone@world.com"/> <form:errors
                            path="emailAddress" cssClass="text-error"/>
                    </div>
                </div>
                <sec:authorize access="hasRole('ROLE_CAOP')">
                    <div class="form-group">
                        <div class="col-xs-3 col-lg-3">
                            <strong>Email Address is Null</strong>
                        </div>
                        <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                            <form:checkbox path="searchNullEmailAddress"/>&nbsp;&nbsp;<font class="muted">(if checked,
                            this will override email search string above)</font>
                        </div>
                    </div>
                </sec:authorize>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Serial Number</strong> <font class="muted" style="text-decoration: underline">(if given,
                        other search criteria are ignored)</font>&nbsp;&nbsp;
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:input path="serial" class="form-control"
                                    placeholder="1234"/> <form:errors
                            path="serial" cssClass="text-error"/>
                    </div>
                </div>
                <!--<a href="#" data-toggle="tooltip"
                title="Allowed chars: a-z A-Z 0-9_ -">
                Role
                </a>-->
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Role</strong>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="role" class="form-control">
                            <form:option value="all"/>
                            <form:option value="User"/>
                            <form:option value="RA Operator"/>
                            <form:option value="CA Operator"/>
                        </form:select>
                    </div>
                </div>
                <!--<a href="#" data-toggle="tooltip"
                title="Allowed chars: a-z A-Z 0-9_ -">
                Status
                </a>-->
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Status</strong> <font class="muted">(note, VALID Certs can be Expired)</font>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="status" class="form-control">
                            <form:option value="VALID"/>
                            <form:option value="REVOKED"/>
                            <form:option value="SUSPENDED"/>
                            <form:option value="all"/>
                        </form:select>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Valid Only</strong> <font class="muted">(Excludes Expired Certs)</font>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:checkbox path="notExpired"/>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-xs-3 col-lg-3">
                        <strong>Results per page:</strong>
                    </div>
                    <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                        <form:select path="showRowCount" class="form-control">
                            <form:option value="20"/>
                            <form:option value="50"/>
                            <form:option value="100"/>
                        </form:select>
                    </div>
                </div>
                <div class="form-group">
                    <div class="col-xs-offset-3">
                        <button type="submit" class="btn btn-md btn-primary">Search</button>
                    </div>
                </div>
            </form:form>
            <br/>

            <h4>Certificate Results (total = ${sessionScope.certSearchPageHolder.totalRows}
                <c:if test="${sessionScope.lastCertSearchDate_session != null}">
                    ,&nbsp;${sessionScope.lastCertSearchDate_session}
                </c:if>)
            </h4>

            <br/>
            <div class="col-xs-11">
                <table id="certResultsTable" class="tablesorter-blue"><!-- table table-hover table-condensed-->
                    <!--  <caption>List of certificate rows returned by search</caption> -->
                    <thead>
                    <tr>
                        <th class="sorter-false">#</th>
                        <!--                                <th>Detail</th>-->
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
                            <!--                                    <td><a href="#" class="classLinkShowHide">Detail</a></td>-->
                            <!--                                    <td>
                                            <a href="#a${count}" 
                                               onclick="showHide('div_${count}');toggleMessage('div_a${count}');">
                                                <div id="div_a${count}">+Show Detail</div>
                                            </a>
                                        </td>-->
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
                                <%--${cert.notAfter}--%>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${cert.dn}">DN
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
                                Showing:&nbsp;[<b>0</b>]
                            </c:if>
                            <c:if test="${sessionScope.certSearchPageHolder.totalRows > 0}">
                                Showing:&nbsp;[<b>${sessionScope.certSearchPageHolder.row+1}</b>]
                            </c:if>
                            to
                            [<b>${sessionScope.certSearchPageHolder.row + fn:length(sessionScope.certSearchPageHolder.source)}</b>]
                            of [<b>${sessionScope.certSearchPageHolder.totalRows}</b>] <!--zero offset list -->
                        </td>
                        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                        <form:form method="post" action="${pageContext.request.contextPath}/raop/searchcert/goto"
                                   modelAttribute="gotoPageFormBean">
                            <td>Go to row:</td>
                            <td>
                                <form:input path="gotoPageNumber" cssStyle="width:30px" placeholder="0"/>
                                <button type="submit" class="btn btn-sm">Go</button>
                            </td>
                        </form:form>
                        <td>&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;</td>
                        <c:if test="${fn:length(sessionScope.certSearchPageHolder.source) > 0}">
                            <c:url value="/raop/searchcert/page?page=next" var="pagenextaction"/>
                            <c:url value="/raop/searchcert/page?page=prev" var="pageprevaction"/>
                            <c:url value="/raop/searchcert/page?page=first" var="pagefirstaction"/>
                            <c:url value="/raop/searchcert/page?page=last" var="pagelastaction"/>
                            <td>
                                <ul class="pager">
                                    <li><a href="${pagefirstaction}">First</a></li>
                                    <li><a href="${pageprevaction}">&laquo; Previous</a></li>
                                    <li><a href="${pagenextaction}">Next &raquo;</a></li>
                                    <li><a href="${pagelastaction}">Last</a></li>
                                </ul>
                            </td>
                        </c:if>
                    </tr>
                </table>
            </div>
        </div> <!-- /container -->
    </div>
</div> <!-- /span -->


<%--<jsp:include page="../common/footer.jsp" />--%>
<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript"
        src="${pageContext.request.contextPath}/resources/jquery/tablesorter/js/jquery.tablesorter.min.js"></script>
<%--<script type="text/javascript" src="${pageContext.request.contextPath}/resources/jquery/tablesorter/js/jquery.tablesorter.widgets.min.js"></script>--%>

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
<script>
    /*function showHide(id) {
       console.log("Info"+id);
       var e = document.getElementById(id);
       if(e.style.display == 'block')
          e.style.display = 'none';
       else
          e.style.display = 'block';
   }

//For use with downtimes active and imminent in the gocdb portal. Will change
// the user control depending on whether the user has expanded the extra information
// table or not
function toggleMessage(id) {
   //console.info(id);
   content = document.getElementById(id).innerHTML;

   if(content.indexOf("+") !== -1){
       //console.log("Found +");
       document.getElementById(id).innerHTML = "-Hide Detail";
   }else{
       //console.log("Not Found +");
       document.getElementById(id).innerHTML = "+Show Detail";
   }

}*/
</script>
</body>
</html>