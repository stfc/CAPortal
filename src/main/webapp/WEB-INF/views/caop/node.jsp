<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%@page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/fmt" prefix="fmt" %>
<%--
<%@ taglib uri="http://www.springframework.org/tags" prefix="s"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>
--%>

<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>CA Operator Import/Export</title>
    <meta name="viewport" content="width=device-width, initial-scale=1">
    <meta name="description" content=""/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap">
    <div class="container">
        <h2>CA Operator Import/Export</h2>
        <div class="row">
            <div class="col-xs-12">
                <form method="post">
                    <!-- do Checks with JSTL and not JS - JS variables are
                    cached which could wrongly show a button -->
                    <c:if test="${disableExport == true}">
                        <input type="submit" value="Export" disabled class="btn btn-primary"/>
                    </c:if>
                    <c:if test="${disableExport == false}">
                        <input type="submit" value="Export" class="btn btn-primary"
                               formaction="${pageContext.request.contextPath}/caop/node/export"/>
                    </c:if>

                    <c:if test="${disableImport == true}">
                        <input type="submit" value="Import" disabled class="btn btn-primary"/>
                    </c:if>
                    <c:if test="${disableImport == false}">
                        <input type="submit" value="Import" class="btn btn-primary"
                               formaction="${pageContext.request.contextPath}/caop/node/import"/>
                    </c:if>
                </form>
            </div>
        </div>

        <br/>
        <br/>


        <div class="row">
            <div class="col-xs-12">
                <h3>Approved CSRs</h3>
                <table id="csrApprovedTable" class="table table-striped table-hover">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Type</th>
                        <th>Serial</th>
                        <th>Submitted On</th>
                        <th>Email</th>
                        <th>CN</th>
                        <th>DN</th>
                        <th>bulkID</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:set var="count" value="0" scope="page"/>
                    <c:forEach var="csr" items="${approvedCsrRows}">
                        <c:url value="/raop/viewcsr?requestId=${csr.req_key}" var="viewcsr"/>
                        <c:set var="count" value="${count + 1}" scope="page"/>
                        <tr>
                            <td>${count}</td>
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
                            <td>${csr.bulk}</td>
                        </tr>
                    </c:forEach>
                    </tbody>
                </table>

            </div>
        </div>


        <div class="row">
            <div class="col-xs-12">
                <h3>Approved CRRs</h3>
                <table id="crrApprovedTable" class="table table-striped table-hover">
                    <thead>
                    <tr>
                        <th>#</th>
                        <th>Type</th>
                        <th>Serial</th>
                        <th>Submitted On</th>
                        <th>Email</th>
                        <th>CN</th>
                        <th>DN</th>
                    </tr>
                    </thead>
                    <tbody>
                    <c:set var="count" value="0" scope="page"/>
                    <c:forEach var="csr" items="${approvedCrrRows}">
                        <c:url value="/raop/viewcrr?requestId=${crr.crr_key}" var="viewcrr"/>
                        <tr>
                            <td>${count}</td>
                            <td>${crr.status}</td>
                            <td><a href="${viewcrr}">${crr.crr_key}</a></td>
                            <td><fmt:formatDate value="${crr.dataSubmit_Date}"/></td>
                            <td><a href="mailto:${crr.email}">${crr.email}</a></td>
                            <td>${csr.cn}</td>
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
            </div>
        </div>


    </div>
</div>
<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript">
    $(document).ready(function () {

        /*if (
        ${disableExport} === true) {
                    $('#exportButton').attr("disabled", true);
                }

                if (
        ${disableImport} === true) {
                    $('#importButton').attr("disabled", true);
                }*/
    });
</script>

</body>
</html>
