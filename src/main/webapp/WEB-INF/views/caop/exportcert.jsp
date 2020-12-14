<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>

<!DOCTYPE html>
<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Export</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Export approved certificates"/>
    <meta name="author" content="Sam Worley"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
    <link href="${pageContext.request.contextPath}/resources/jquery/tablesorter/css/theme.blue.css" rel="stylesheet"/>
</head>
<body>
<%@ include file="../../jspf/header.jspf" %>
<div class="modal fade" id="helpModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel"
     aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content"></div>
    </div>
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title" id="helpModalLabel">Export</h4>
            </div>
            <div class="modal-body">
            </div>
            <div class="modal-footer">
                Click anywhere off this panel to close
            </div>
        </div>
    </div>
</div>
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1">
            <div class="col-xs-10"><h2>Export Approved Certificates</h2>
                <h4>Approved certificates to be exported for signing will appear below</h4>
            </div>
            <div class="col-xs-offset-11">
                <a href="#" id="helpMod" style="color: inherit;">
                    <span class="helperIcon glyphicon glyphicon-question-sign" style="font-size: xx-large;"></span>
                </a>
            </div>
            <div class="col-xs-11">
                <br/>
                [<b>${fn:length(approved_reqrows)}</b>] Approved <b>CSRs</b> [<b>${fn:length(crr_reqrows)}</b>] Approved
                <b>CRRs</b>
                &nbsp;&nbsp;&nbsp;&nbsp;Last Refreshed: <b>${lastPageRefreshDate}</b>
                <br/><br/>
                <table class="table tablecondensed">
                    <thead>
                    <tr>
                        <th>Type</th>
                        <th>Serial</th>
                        <th>Name</th>
                        <th>Submitted On</th>
                        <th>DN</th>
                    </tr>
                    </thead>
                    <c:forEach items="${approved_reqrows}" var="row">
                        <c:url value="/raop/viewcsr?requestId=${row.req_key}" var="viewreq"/>
                        <tr>
                            <td>CSR</td>
                            <td><a href="${viewreq}">${row.req_key}</a></td>
                            <td>${row.cn}</td>
                            <td>${row.dataNotBefore}</td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${row.dn}">DN
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                    <c:forEach items="${crr_reqrows}" var="row">
                        <c:url value="/raop/viewcrr?requestId=${row.crr_key}" var="viewcrr"/>
                        <tr>
                            <td>CRR</td>
                            <td><a href="${viewcrr}">${row.crr_key}</a></td>
                            <td>${row.cn}</td>
                            <td>${row.dataSubmit_Date}</td>
                            <td class="vertAlign">
                                <button type="button" class="btn btn-sm dnPop" data-container="body"
                                        data-toggle="popover"
                                        data-placement="top" data-content="${row.dn}">DN
                                </button>
                            </td>
                        </tr>
                    </c:forEach>
                </table>
                <button id="exportBt" type="submit" class="btn btn-md btn-primary"
                        data-toggle="tooltip" data-placement="right"
                        title="Export CSRs and CRRs to be signed"
                        onclick="return confirm('Are you sure you want to export approved requests for signing?');">
                    Export - NOT IMPLEMENTED YET
                </button>
            </div>
        </div>
    </div>
</div>

<%@ include file="../../jspf/footer.jspf" %>
<script type="text/javascript">
    $('#exportBt').tooltip();
    $("#helpMod").click(function () {
        $('#helpModal').modal('show');
    });
</script>
</body>
</html>
