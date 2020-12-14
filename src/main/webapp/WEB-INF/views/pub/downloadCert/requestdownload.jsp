<%@page contentType="text/html" pageEncoding="windows-1252" %>
<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<!DOCTYPE html>

<html>

<head>
    <meta http-equiv="Content-Type" content="text/html; charset=windows-1252">
    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Download Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Page for downloading certificates that were requested via the portal."/>
    <meta name="author" content="David Meredith"/>
    <meta name="author" content="Sam Worley"/>
    <!-- Styles -->
    <%@ include file="../../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body>
<%@ include file="../../../jspf/header.jspf" %>
<div class="modal fade" id="helpModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel"
     aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content"></div>
    </div>
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title" id="helpModalLabel">Download Certificate</h4>
            </div>
            <div class="modal-body">
                <strong>To download a certificate:</strong>
                <ol>
                    <li>Enter the <strong>certificate serial number</strong>
                        (this was e-mailed to you when your certificate was created)
                    </li>
                    <li>Enter the <strong>e-mail address</strong> that you used
                        to request the certificate
                    </li>
                    <li>Click <strong>'Download Certificate'</strong></li>
                </ol>
                <br/>
                <br/>
                <strong>Create a certificate file that can be imported into your web browser:</strong><br/>
                After the certificate is downloaded, you can optionally create a
                <strong>browser certificate file</strong> for importing into a web browser (a file with a '.p12'
                extension
                that contains both your certificate and private key).
                <ol>
                    <li><strong>Complete the form and click 'Download Certificate'</strong> as above</li>
                    <li><strong>Select your private key</strong>. Either paste your privateKey
                        text into the text area or browse for your local private key file you saved when
                        requesting your certificate
                    </li>
                    <li><strong>Provide your password</strong> - this will decrypt your privateKey</li>
                    <li><strong>Click 'Save Certificate'</strong> and save your browser certificate file in a safe place
                    </li>
                    <li>Import the .p12 file into your web browser</li>
                </ol>
                You can repeat the above process any time you need to download your certificate.
            </div>
            <div class="modal-footer">
                Click anywhere off this panel to close
            </div>
        </div>
    </div>
</div>

<!--        <div id="IE11warning" class="col-xs-11 col-lg-10 alert alert-danger" role="alert" style="display: none;">
            You appear to be using IE 11 - there is a bug in IE 11 which prevents the creation of a certificate bundle (p12) file.  
            <br/>Please use another browser. We are looking into a solution. Apologies for the inconvenience. 
        </div>-->

<!-- Wrap all page content here -->
<div id="wrap">
    <div class="row">
        <div class="col-xs-offset-1">
            <div class="row">
                <div class="col-xs-10"><h2>Download Certificate</h2></div>
                <div class="col-xs-offset-11">
                    <a href="#" id="helpMod" style="color: inherit;">
                        <span class="helperIcon glyphicon glyphicon-question-sign" style="font-size: xx-large;"></span>
                    </a>
                </div>
            </div>
            <div>
                <c:if test="${not empty successMessage}">
                    <div class="success">
                        <ul>
                            <li>${successMessage}</li>
                            <li><strong>Follow Steps 1,2,3 below </strong>to create a browser compatible certificate
                                file
                        </ul>
                    </div>
                </c:if>
                <c:if test="${not empty errorMessage}">
                    <div id="message" class="error">Error - ${errorMessage}</div>
                </c:if>
            </div>


            <c:if test="${cert == null}">
                <br/>
                <form:form id="requestCertForm" method="post"
                           action="${pageContext.request.contextPath}/pub/downloadCert/requestdownload"
                           modelAttribute="requestDownloadCertFormBean" cssClass="form-horizontal">
                    <div>
                        <s:bind path="*">
                            <c:if test="${status.error}">
                                <div id="message" class="error">Form has errors</div>
                            </c:if>
                        </s:bind>
                    </div>
                    <div class="form-group">
                        <div class="col-xs-3 col-lg-2">
                            <strong>Certificate Serial Number</strong>
                        </div>
                        <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                            <form:input id="certId" class="form-control" path="certId" placeholder="12345"/>
                            <span></span>
                            <form:errors path="certId" cssClass="text-error"/>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-xs-3 col-lg-2">
                            <strong>Certificate Email Address</strong>
                        </div>
                        <div class="col-xs-8 col-sm-6 col-md-5 col-lg-3">
                            <form:input id="email" class="form-control" path="email" placeholder="some.body@world.com"/>
                            <span></span>
                            <form:errors path="email" cssClass="text-error"/>
                        </div>
                    </div>
                    <div class="form-group">
                        <div class="col-xs-offset-2 col-xs-8">
                            <button id="submitButton" type="submit" class="btn btn-sm btn-primary">
                                Download Certificate
                            </button>
                        </div>
                    </div>
                </form:form>
            </c:if>

            <c:if test="${cert != null}">
                <div class="form-group">
                    <div class="col-xs-11">
                        <table class="table table-hover table-condensed">
                            <thead>
                            <tr>
                                <th>Certificate Attribute</th>
                                <th>Value</th>
                            </tr>
                            </thead>
                            <tbody>
                            <tr>
                                <td>Serial Number (cert_key)</td>
                                <td>${cert.cert_key}, (hex: ${hexSerial})</td>
                            </tr>
                            <tr>
                                <td>Common Name (CN)</td>
                                <td>${cert.cn}</td>
                            </tr>
                            <tr>
                                <td>Distinguished Name (DN)</td>
                                <td>${cert.dn}</td>
                            </tr>
                            <tr>
                                <td>Issuer DN</td>
                                <td>${certObj.issuerDN}</td>
                            </tr>
                            <tr>
                                <td>Email</td>
                                <td><a href="mailto:${cert.email}">${cert.email}</a></td>
                            </tr>
                            <tr>
                                <td>Status</td>
                                <td><b>
                                    <c:if test="${cert.status == 'VALID'}">
                                        <font color="green">${cert.status}</font>
                                    </c:if>
                                    <c:if test="${cert.status != 'VALID'}">
                                        <font color="red">${cert.status}</font>
                                    </c:if>
                                </b></td>
                            </tr>
                            <tr>
                                <td>Role</td>
                                <td>${cert.role}</td>
                            </tr>
                            <tr>
                                <td>Not Before (Starts)</td>
                                <td>${certObj.notBefore}</td>
                            </tr>
                            <tr>
                                <td>Not After (Expires)</td>
                                <td>${certObj.notAfter}</td>
                            </tr>

                            <tr>
                                <td>Signature Algorithm</td>
                                <td>${certObj.sigAlgName}</td>
                            </tr>
                            <tr>
                                <td>Type/Version</td>
                                <td>${certObj.type} / ${certObj.version}</td>
                            </tr>
                            <tr>
                                <td>Cert PEM</td>
                                <td><textarea id="certpem" class="form-control" readonly
                                              style="height: 160px;">${certdata}</textarea></td>
                            </tr>
                                <%--<tr>
                                    <td>
                                        <a id="refreshButton" class="btn btn-sm btn-info"
                                           href="${pageContext.request.contextPath}/pub/downloadCert/requestdownload">
                                            Clear/Refresh Page
                                        </a>
                                    </td>
                                    <td>&nbsp;</td>
                                </tr>--%>


                            <tr>
                                <td>
                                    <img src="${pageContext.request.contextPath}/resources/images/number1.png"
                                         alt="number 1" style="width: 100px; height: 100px;"/>
                                    Provide Your Private Key
                                </td>
                                <td>
                                    <div class="text-info">
                                        <!--                                                    <strong>Provide Your Private Key</strong> -->
                                        <ul>
                                            <li>Paste file contents or browse for file</li>
                                            <li>This file was saved when applying for the certificate using this
                                                portal
                                            </li>
                                            <li>Your key and pw are <strong>NEVER</strong> sent over to the server
                                                <a href="#" id="howLink" data-toggle="tooltip" data-placement="right"
                                                   title="We use local JavaScript that runs in your browser to create the .p12 file">(how?)</a>
                                            </li>
                                        </ul>
                                    </div>
                                    <textarea class="form-control" id="certkey"
                                              placeholder="Paste your private key here or browse for your private key text file, then provide the password and click 'Save Certificate (.p12)."
                                              style="height: 160px;" data-toggle="tooltip" data-placement="top"
                                              title="Paste or browse for your private key text file (privateKeyAndCSR.txt). Note, the privateKey is NOT sent to the server.">
                                                </textarea><span></span>
                                    <input type="file" onchange="loadfile(this)" id="uploadText" data-toggle="tooltip"
                                           data-placement="right"
                                           title="Browse for your private key text file. Note, this file is NOT sent to the server"/>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <img src="${pageContext.request.contextPath}/resources/images/number2.png"
                                         alt="number 2" style="width: 100px; height: 100px;"/>
                                    Enter Your Private Key Password
                                </td>
                                <td>
                                    <!--                                                <div class="text-info">
                                                                                        <strong>Enter Your Private Key Password</strong>
                                                                                    </div>-->
                                    <br/>
                                    <input type="password" id="keypass" class="form-control" data-placement="right"
                                           title="Password entered when applying for the certificate"/>
                                    <span></span>
                                </td>
                            </tr>
                            <tr>
                                <td>
                                    <img src="${pageContext.request.contextPath}/resources/images/number3.png"
                                         alt="number 3" style="width: 100px; height: 100px;"/>
                                    Click 'Save Certificate' button
                                </td>
                                <td>
                                    <div class="text-info">
                                        <!--                                                    <strong>Click 'Save Certificate' button</strong>-->
                                        <ul>
                                            <li>Creates a local Certificate Bundle file (.p12 file)</li>
                                            <li>The .p12 file can be imported into a web browser</li>
                                        </ul>
                                    </div>
                                    <a id="createP12Button" class="btn btn-sm btn-primary" data-toggle="tooltip"
                                       data-placement="right"
                                       title="Will prompt .p12 download if valid private key and password entered">
                                        Save Certificate (.p12)
                                    </a>
                                    <a href="#" id="mydownloadURI" class="btn btn-sm btn-primary" download="mycert.p12">Download</a>
                                    <a id="createFlashButton"></a>
                                    <a id="refreshButton" class="btn btn-sm btn-info"
                                       href="${pageContext.request.contextPath}/pub/downloadCert/requestdownload">
                                        Clear/Refresh Page
                                    </a>
                                </td>
                            </tr>


                            </tbody>
                        </table>
                    </div>
                </div>
                <br/>
                <br/>
                <br/>


                <%--<div class="form-horizontal">
                    <div class="form-group">
                        <div class="col-xs-1 col-lg-1">&nbsp;
                        </div>
                        <div class="col-xs-10">
                            <h3>Follow Steps 1,2,3 to Save as a Browser Certificate File</h3>
                            <p class="text-info">Note, your private-key and password are <strong>NEVER</strong> sent over the wire to the server
                                <a href="#" id="howLink" data-toggle="tooltip" data-placement="right"
                                   title="We use local JavaScript that runs in your browser to create the .p12 file">(how?)</a>
                            </p>
                        </div>
                    </div>

                    <div class="form-group">
                        <div class="col-xs-2 col-lg-1">
                            <img src="${pageContext.request.contextPath}/resources/images/number1.png" alt="number 1" style="width: 100px; height: 100px;"/>
                        </div>
                        <div class="col-xs-9 col-lg-10">
                            Provide Your Private Key (saved when applying for the certificate using this portal)<br/>
                            <textarea class="form-control" id="certkey"
                                      placeholder="Paste your private key here or browse for your private key text file, then provide the password and click 'Save Certificate (.p12)."
                                      style="height: 160px;" data-toggle="tooltip" data-placement="top"
                                title="Paste or browse for your private key text file (privateKeyAndCSR.txt). Note, the privateKey is NOT sent to the server.">
                            </textarea><span></span>
                            <input type="file" onchange="loadfile(this)" id="uploadText" data-toggle="tooltip" data-placement="right"
                                   title="Browse for your private key text file. Note, this file is NOT sent to the server" />
                        </div>
                    </div>


                   <div class="form-group">
                        <div class="col-xs-2 col-lg-1">
                            <img src="${pageContext.request.contextPath}/resources/images/number2.png" alt="number 2" style="width: 100px; height: 100px;"/>
                        </div>
<!--                                <div class="col-xs-9 col-lg-10">-->
                        <div class="col-xs-7 col-md-4 col-lg-3">
                            Enter Your Private Key Password<br/>
                            <input type="password" id="keypass" class="form-control" data-placement="right" title="Password entered when applying for the certificate"/><span></span>
                        </div>
                    </div>

                    <div class="form-group">
                        <div class="col-xs-2 col-lg-1">
                            <img src="${pageContext.request.contextPath}/resources/images/number3.png" alt="number 3" style="width: 100px; height: 100px;"/>
                        </div>
                        <div class="col-xs-9">
                            Click to save your local Certificate Bundle (.p12 files can be imported into browsers)<br/>
                            <a id="createP12Button" class="btn btn-sm btn-primary" data-toggle="tooltip" data-placement="right" title="Will prompt .p12 download if valid private key and password entered">
                                Save Certificate (.p12)
                            </a>
                            <a href="#" id="mydownloadURI" class="btn btn-sm btn-primary" download="mycert.p12">Download</a>
                            <a id="createFlashButton"></a>
                            <a id="refreshButton" class="btn btn-sm btn-info"
                                           href="${pageContext.request.contextPath}/pub/downloadCert/requestdownload">
                                            Clear/Refresh Page
                                        </a>
                        </div>
                    </div>
            </div>--%>

            </c:if>


        </div>
    </div>
</div>

<!-- footer includes shared .js files -->
<%@ include file="../../../jspf/footer.jspf" %>
<!-- Stuff for crypto / csrs -->
<script src="https://cdn.jsdelivr.net/npm/node-forge@0.7.0/dist/forge.min.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/base64.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/Blob.js"></script>
<!-- https://github.com/eligrey/FileSaver.js -->
<script src="${pageContext.request.contextPath}/resources/javascript/FileSaver.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/crypto.js"></script>

<script type="text/javascript">
    function pwValid() {
        //var element = $("#" + pwelement.attr("id"));
        var element = $("#keypass");
        if ($(element).val().length < 4) {
            $(element).addClass("error").next().text("Password too short");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function keyValid() {
        var element = $("#certkey");
        //error class applied if regex is not matched in private key field
        if (!$(element).val().match(/(-----BEGIN \b(ENCRYPTED|RSA)\b PRIVATE KEY-----)[\s\S]*(-----END \b(ENCRYPTED|RSA)\b PRIVATE KEY-----)/)) {
            $(element).addClass("error"); //.next().text("Please paste private key");
            return false;
        } else {
            $(element).removeClass("error"); // .next().text("");
            return true;
        }
    }

    function browserCheck(b64p12) {
        var myBuffer = base64DecToArr(b64p12).buffer;
        var p12blob = new Blob([myBuffer], {type: 'application/octet-stream'});
        saveAs(p12blob, "certBundle.p12");
    }

    function loadfile(input) {
        var fileReader = new FileReader();
        fileReader.onload = function (fileLoadedEvent) {
            var textFromFileLoaded = fileLoadedEvent.target.result;
            $("#certkey").val(textFromFileLoaded);
            keyValid();
        };
        fileReader.readAsText(input.files[0], "UTF-8");
    }

    $(document).ready(function () {

//                if (ie_ver() === 11) {
//                    $("#IE11warning").show();
//                    $("#createCSRSubmit").attr('disabled', 'disabled');
//                }

        //$("#keypass").tooltip();
        $('#howLink').tooltip();
        $("#certkey").tooltip();
        $("#createP12Button").tooltip();
        $("#uploadText").tooltip();
        $("#keypass").blur(function () {
            pwValid();
        });
        $("#keypass").keyup(function () {
            pwValid();
        });
        $("#certkey").blur(function () {
            keyValid();
        });
        $("#certkey").keyup(function () {
            keyValid();
        });
        $("#helpMod").click(function () {
            $('#helpModal').modal('show');
        });
        $("#createP12Button").click(function () {
            //error checking to ensure all fields are filled in
            if ($('#keypass').val() === "" && $('#certkey').val() === "=") {
                alert("Please enter private key password and use 'Choose File' and 'Load File' buttons to select private key (or paste private key into textarea)");
            } else if ($('#keypass').val() === "") {
                alert("Please enter private key password");
            } else if ($('#certkey').val() === "") {
                alert("Please use 'Choose File' and 'Load File' buttons to select your private key (or paste private key into textarea)");
            } else {
                //regex to ensure only private key used from text pasted in
                var regexPrivKey = $("#certkey").val().match(/(-----BEGIN \b(ENCRYPTED|RSA)\b PRIVATE KEY-----)[\s\S]*(-----END \b(ENCRYPTED|RSA)\b PRIVATE KEY-----)/);
                if (regexPrivKey === null) {
                    alert("Private key not found, please ensure correct file has been selected or pasted into text area.");
                } else {
                    var _pem = {
                        certificateDev: $("#certpem").val(),
                        privateKeyDev: regexPrivKey[0]
                    };
                    // Try to parse certificate and private key object
                    try {
                        var cert = forge.pki.certificateFromPem(_pem.certificateDev, true);
                        var password = $('#keypass').val();
                        var privateKey = forge.pki.decryptRsaPrivateKey(_pem.privateKeyDev, password);
                        if (privateKey !== null) {
                            var chain = [cert];
                            //create PKCS12
                            console.log('\nCreating PKCS#12...');
                            var newPkcs12Asn1 = forge.pkcs12.toPkcs12Asn1(privateKey, chain, password,
                                {generateLocalKeyId: true, friendlyName: 'myUkCaCertficate', algorithm: '3des'});
                            var newPkcs12Der = forge.asn1.toDer(newPkcs12Asn1).getBytes();
                            console.log('\nBase64-encoded new PKCS#12:');
                            var b64p12 = forge.util.encode64(newPkcs12Der);
                            browserCheck(b64p12);
                        } else {
                            alert("Please enter the correct password for the private key");
                        }
                    } catch (ex) {
                        alert("Could not create private key object - please check provided password and private key text");
                        if (ex.stack) {
                            console.log(ex.stack);
                        } else {
                            console.log('Error', ex);
                        }
                    }
                }
            }
        });


    });  // end on ready
</script>
</body>
</html>
