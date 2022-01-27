
<%@ page language="java" contentType="text/html; charset=ISO-8859-1" pageEncoding="ISO-8859-1" %>
<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<!doctype html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=ISO-8859-1"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Verify Pin</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="add desc here"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>

<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">

            <h2>Verify Pin</h2>
            <c:if test="${pin == null}">
                No Pin to verify
            </c:if>

            <c:if test="${pin != null}">

                Pin Code: ${pin}
                <br/>

                <table>
                    <form:form method="post" action="${pageContext.request.contextPath}/raop/verifyPin"
                               modelAttribute="verifyPinFormBean">
                        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
                        <tr>
                            <td>
                                <br/>
                                <form:password path="pinVerification" class="form-control"
                                               placeholder="enter original pin"/>
                            </td>
                            <td>
                                <form:errors path="pinVerification" cssClass="text-error"/>
                            </td>
                        </tr>
                        <tr>
                            <td>
                                <input name="pin" type="hidden" value="${pin}"/>
                                <br/>
                                <button type="submit" class="btn btn-sm btn-primary">Verify</button>
                            </td>
                            <td></td>
                        </tr>
                    </form:form>
                </table>

                <!-- If the pin verified ok, show a message -->
                <c:if test="${not empty verifiedOk}">
                    <div class="success">${verifiedOk}</div>
                </c:if>
                <c:if test="${not empty notVerifiedOk}">
                    <div class="error">${notVerifiedOk}</div>
                </c:if>


            </c:if>


        </div>
    </div>
</div>



<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>
