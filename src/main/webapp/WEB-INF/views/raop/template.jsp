
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
    <title>title here</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="add desc here"/>
    <meta name="author" content="David Meredith"/>
    <!-- Styles -->
    <%--<jsp:include page="../common/styles.jsp" />--%>
    <%@ include file="../../jspf/styles.jspf" %>
</head>
<body>
<%--<jsp:include page="../common/header.jsp" />--%>
<%@ include file="../../jspf/header.jspf" %>
Content here


<%@ include file="../../jspf/footer.jspf" %>
</body>
</html>
