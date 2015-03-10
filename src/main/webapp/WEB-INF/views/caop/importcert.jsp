<%@page contentType="text/html" pageEncoding="windows-1252"%>
<%@ page session="false"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c"%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn"%>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s"%>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form"%>

<!DOCTYPE html>
<html>

    <head>
        <meta http-equiv="Content-Type" content="text/html; charset=windows-1252"/>
        <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/> 
        <title>Import</title>
        <meta name="viewport" content="width=device-width, initial-scale=1.0" />
        <meta name="description" content="Export approved certificates" />
        <meta name="author" content="Sam Worley" />
        <!-- Styles -->
        <%@ include file="../../jspf/styles.jspf" %>
        <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet" />
        <link href="${pageContext.request.contextPath}/resources/jquery/tablesorter/css/theme.blue.css" rel="stylesheet" />
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
                        <h4 class="modal-title" id="helpModalLabel">Import</h4>
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
                    <div class="col-xs-10"><h2>Import Signed Certificates</h2>
                    </div>
                    <div class="col-xs-offset-11">
                            <a href="#" id="helpMod" style="color: inherit;">
                                <span class="helperIcon glyphicon glyphicon-question-sign" style="font-size: xx-large;"></span>
                            </a>
                    </div>
                    <br/><br/>
                    <div class="col-xs-11">
                        <button id="importBt" type="submit" class="btn btn-md btn-warning"
                                data-toggle="tooltip" data-placement="right"
                                title="Import New Certificates and CRL"
                                onclick="return confirm('Are you sure you want to upload approved requests?');">Import - NOT IMPLEMENTED YET
                        </button>
                    </div>
                </div>
            </div>
        </div>
    <%@ include file="../../jspf/footer.jspf" %>
    <script type="text/javascript">
        $('#importBt').tooltip();
        $("#helpMod").click(function() {
            $('#helpModal').modal('show');
        });
    </script>
    </body>
</html>