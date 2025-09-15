
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>

<!doctype html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>RAOP role assignment</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="RAOP role assignment page for UK RA operators"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
</head>

<body>
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">

            <h2>List of users and RA operator for your RA [<span class="text-info">${ra}</span>]</h2>
            <div class="col-11">
                <table class="table tablecondensed">
                    <thead>
                    <tr>
                        <th>Name</th>
                        <th>Role</th>
                        <th>Request</th>
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
                                        <button id="revokeRaopRole" type="button" class="btn btn-sm btn-warning" data-content="${row.dn}">
                                            Revoke
                                        </button>                                    
                                    </c:when>
                                    <c:otherwise>    
                                        <button id="assignRaopRole" type="button" class="btn btn-sm btn-primary" data-content="${row.dn}">
                                            Assign
                                        </button>                                    
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

<script type="text/javascript">
    $(document).ready(function () {
        $("#revokeRaopRole").click(function () {
            if (!window.confirm("Are you sure you want to request revocation of RA Operator role ?")) {
                return;
            }
        });

        $("#assignRaopRole").click(function () {
            if (!window.confirm("Are you sure you want to request approval for RA Operator role?")) {
                return;
            }
        });
    });
</script>
</body>
</html>