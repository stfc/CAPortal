<%-- 
    Document   : error
    Created on : 04-Jun-2014, 09:25:18
    Author     : djm76
--%>


<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>

<!doctype html>
<html>
<head>

    <title>Error Occurred</title>
</head>
<body>
<!--
An error page with hidden stack-trace suitable for tech support.
-->
<h1>Application Error</h1>

<!-- You could use ${requestScope['jakarta.servlet.forward.request_uri']}
        but it's a lot more verbose and doesn't give you the full page URL. -->
<c:if test="${not empty url}">
    <p>
        <b>Page:</b> ${url}
    </p>
</c:if>

<c:if test="${not empty timestamp}">
    <p id='created'>
        <b>Occurred:</b> ${timestamp}
    </p>
</c:if>

<c:if test="${not empty status}">
    <p>
        <b>Response Status:</b> ${status}
        <c:if test="${error}">(${error})</c:if>
    </p>
</c:if>

<p>
    Oh dear, the application has encountered an error.
</p>
<p>
    Please would you contact <a href="mailto:support@grid-support.ac.uk">support@grid-support.ac.uk</a>
    and copy/paste the error details shown in the text area below ('Ctrl+a' to select all then 'Ctrl+c' to copy).
</p>
<p>Apologies for the inconvenience caused.</p>


<textarea style="width: 100%; height: 200px;">
        Failed URL: ${url}
    Exception: ${exception.message}
    <c:forEach items="${exception.stackTrace}" var="ste"> ${ste}
    </c:forEach>
        </textarea>

</body>
</html>
