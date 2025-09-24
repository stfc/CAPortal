
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<!doctype html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Manage RAOP role</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="RAOP role management page for UK RA operators"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">

            <h2>List of users and RA operator for your RA [<span class="text-info">${ra}</span>]</h2>
            Last Refreshed: <b>${lastPageRefreshDate}</b>
            
            <c:if test="${not empty responseMessage}">
                <br/>
                <div class="alert alert-info">
                    ${responseMessage}
                </div>
            </c:if>
            <c:if test="${empty responseMessage}">
                <br/><br/><br/>
            </c:if>

            <div class="col-12">
                <table class="table tablecondensed">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Role</th>
                        <th>Action</th>
                        <th>Last updated</th>
                    </tr>
                    </thead>
                    <c:forEach items="${userRaopRows}" var="row">
                        <tr>
                            <td>${row.cn}</td>
                            <td>${row.role}</td>
                            <td>
                                <c:choose>
                                    <c:when test="${row.role == 'RA Operator'}">
                                        <c:choose>
                                            <c:when test="${row.dn == currentUserDn}">
                                                <button type="button" class="btn btn-sm btn-secondary" disabled>
                                                    No Self-Request
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <form:form method="post"
                                                    action="${pageContext.request.contextPath}/raop/manageraop/changeroletouser">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                                    <input type="hidden" name="cert_key" value="${row.cert_key}" />
                                                    <button type="submit" class="btn btn-sm btn-warning"
                                                        onclick="return confirm('Are you sure you want to revoke RA Operator role?')">
                                                        Revoke RAOP role
                                                    </button>
                                                </form:form>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:when>
                                    <c:otherwise>
                                        <c:choose>
                                            <c:when test="${listOfRoleChangeRequestCertKey.contains(row.cert_key)}">
                                                <button type="button" class="btn btn-sm btn-primary" disabled>
                                                    Request pending
                                                </button>
                                            </c:when>
                                            <c:otherwise>
                                                <form:form method="post"
                                                    action="${pageContext.request.contextPath}/raop/manageraop/requestraoprole">
                                                    <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}" />
                                                    <input type="hidden" name="cert_key" value="${row.cert_key}" />
                                                    <button type="submit" class="btn btn-sm btn-primary"
                                                        onclick="return confirm('Are you sure you want to request RA Operator role?')">
                                                        Request RAOP role
                                                    </button>
                                                </form:form>
                                            </c:otherwise>
                                        </c:choose>
                                    </c:otherwise>
                                </c:choose>
                            </td>
                            <td>${row.notAfter}</td>
                        </tr>
                    </c:forEach>
                </table>
            </div>
        </div>
    </div>
</div>
<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>