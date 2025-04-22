
<%@page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<!doctype html>
<html>
<head>

    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Cert Renew</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Renew certificate page for UK CA certificate owners"/>
    <meta name="author" content="David Meredith"/>
    <meta name="_csrf" content="${_csrf.token}"/>
    <!-- default header name is X-CSRF-TOKEN -->
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body id="certBody">
<%@ include file="../../jspf/header.jspf" %>
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">
            <h2>Renew Certificate</h2>
            <ul>
                <li>When clicking 'Submit Request' a new <abbr title="Certificate Signing Request">CSR</abbr> is
                    created by your <strong>browser</strong>.
                </li>
                <li><strong>ONLY the public key</strong> is sent to the server as a <abbr
                        title="Certificate Signing Request">CSR</abbr>.
                    The private key and password are <strong>NOT</strong> sent to the server.
                </li>
                <li>After clicking Submit, you will be asked to <strong>save your private key</strong> - you must do
                    this. Also remember your key <b>password</b>.
                </li>
                <li>You will then be e-mailed when your certificate is ready to download.</li>
                <li>If the browser crashes or a timeout occurs and you are not asked to save the private key, please
                    contact your RA or the CA.
                </li>
            </ul>

            <c:if test="${not empty renewOkMessage}">
                <div class="success">${renewOkMessage}</div>
            </c:if>
            <br/>
            <div class="col-11 col-lg-10">
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
                        <td>${certificateRow.cert_key}, (hex: ${certHexSerial})</td>
                    </tr>
                    <tr>
                        <td>Common Name (CN)</td>
                        <td>${certificateRow.cn}</td>
                    </tr>
                    <tr>
                        <td>Distinguished Name (DN)</td>
                        <td>${cert.subjectDN}</td>
                    </tr>
                    <tr>
                        <td>Issuer DN</td>
                        <td>${cert.issuerDN}</td>
                    </tr>
                    <tr>
                        <td>Email</td>
                        <td><a href="mailto:${certificateRow.email}">${certificateRow.email}</a></td>
                    </tr>
                    <tr>
                        <td>Status</td>
                        <td><b>
                            <c:if test="${certificateRow.status == 'VALID'}">
                                <span class="text-success">${certificateRow.status}</span>
                            </c:if>
                            <c:if test="${certificateRow.status != 'VALID'}">
                                <span class="text-danger">${certificateRow.status}</span>
                            </c:if>
                        </b></td>
                    </tr>
                    <tr>
                        <td>Not Before</td>
                        <td>${cert.notBefore}</td>
                    </tr>
                    <tr>
                        <td>Not After</td>
                        <td>${cert.notAfter}</td>
                    </tr>

                    <tr>
                        <td>Signature Algorithm</td>
                        <td>${cert.sigAlgName}</td>
                    </tr>
                    <tr>
                        <td>Type</td>
                        <td>${cert.type}</td>
                    </tr>
                    <tr>
                        <td>Version</td>
                        <td>${cert.version}</td>
                    </tr>
                    </tbody>
                </table>
            </div>

            <br/>

            <c:if test="${certificateRow.status == 'VALID'}">
                <a id="postLinkViaClient" href="${pageContext.request.contextPath}/cert_owner/renewViaClient"
                   style="display: none;"></a>
                <a id="postLinkViaServer" href="${pageContext.request.contextPath}/cert_owner/renewViaServer"
                   style="display: none;"></a>

                <form action="" class="form-horizontal" role="form">
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="emailInputText">e-Mail</label>
                        </div>
                        <div class="col">
                            <input type="text" id="emailInputText" value="${certificateRow.email}"
                                   class="form-control"/><span></span>
                            <div class="form-text">Email that is associated with your Certificate</div>
                        </div>
                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="sign_up_password">Key Password</label>
                        </div>
                        <div class="col">
                            <input type="password" id="sign_up_password" class="form-control"/><span></span>
                            <div class="form-text">The password is used to encrypt your locally generated private key.</div>
                        </div>
                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="sign_up_password_confirm">Confirm Password</label>
                        </div>
                        <div class="col">
                            <input type="password" id="sign_up_password_confirm" class="form-control"/><span></span>
                        </div>
                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <br />
                            <a id="createCSRSubmit" class="btn btn-sm btn-primary">Submit Request</a>
                            <div class="form-text">This may take some time depending on your browser/computer (it generates a new public/private key-pair in the browser and sends the public key to the server)</div><br />
                            <a id="refreshButton" class="btn btn-sm btn-info">Clear / Refresh</a>
                        </div>
                    </div>
                </form>
            </c:if>
            <div id="responseMessage"></div>
            <div class="col-11">
                <a id="savetxt" class="btn btn-sm btn-primary">
                    Save Private Key As Text File
                </a>
            </div>
            <div class="col-11">
                <textarea id="csrTextArea" style="width: 900px; height: 200px;"></textarea>
            </div>
        </div>
    </div>
</div>

<%@ include file="../../jspf/footer.jspf" %>
<!-- Stuff for crypto / csrs -->
<script src="https://cdn.jsdelivr.net/npm/node-forge@0.10.0/dist/forge.min.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/base64.js"></script>
<!-- https://github.com/eligrey/FileSaver.js -->
<script src="${pageContext.request.contextPath}/resources/javascript/FileSaver.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/crypto.js"></script>

<script type="text/javascript">

    function emailValid() {
        const element = $("#emailInputText");
        //if (element.val().match(/^(([0-9a-zA-Z\+]+[\-\._])*[0-9a-zA-Z]+@([-0-9a-zA-Z]+[.])+[a-zA-Z]{2,6}[,;]?)+$/) === null) {
        if (element.val().match(/^[_A-Za-z0-9-\+]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9]+)*(\.[A-Za-z]{2,})$/) === null) {
            $(element).addClass("error").next().text("Invalid email");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function pwSame() {
        const pw_conf = $("#sign_up_password_confirm");
        const pw = $("#sign_up_password");
        if ($(pw_conf).val() !== $(pw).val()) {
            $(pw_conf).addClass("error").next().text("Passwords don't match");
            return false;
        } else {
            $(pw_conf).removeClass("error").next().text("");
            return true;
        }
    }

    function pwValid() {
        //var element = $("#" + pwelement.attr("id"));
        const element = $("#sign_up_password");
        if ($(element).val().length < 10) {
            $(element).addClass("error").next().text("Password too short");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function enableSubmit() {
        if (pwValid() && pwSame() && emailValid()) {
            return true;
        } else {
            return false;
        }
    }

    function showDownloadLink() {
        $('#savetxt').show();
    }

    $(document).ready(function () {

        $("#sign_up_passwordTitle").tooltip();
        $("#sign_up_password_confirmTitle").tooltip();
        $("#emailTitle").tooltip();
        $("#savetxt").tooltip();
        $("#createCSRSubmit").tooltip();

        $('#savetxt').click(function () {
            const textfile = new Blob([$('#csrTextArea').val()], {type: "text/plain;charset=utf-8"});
            saveAs(textfile, "privateKeyAndCsr.txt");
        });

        $("#refreshButton").click(function () {
            location.reload(true); // force reload from server rather than cache
        });

        $("#sign_up_password").blur(function () {
            pwValid();
            pwSame();
        });
        $("#sign_up_password_confirm").blur(function () {
            pwValid();
            pwSame();
        });
        $("#emailInputText").blur(function () {
            emailValid();
        });

        $("#sign_up_password").keyup(function () {
            pwValid();
            pwSame();
        });
        $("#sign_up_password_confirm").keyup(function () {
            pwValid();
            pwSame();
        });
        $("#emailInputText").keyup(function () {
            emailValid();
        });

        // See:
        // http://api.jquery.com/val/
        // At present, using .val() on textarea elements strips carriage
        // return characters from the browser-reported value. When this
        // value is sent to the server via XHR however, carriage returns
        // are preserved (or added by browsers which do not include them
        // in the raw value). A workaround for this issue can be achieved
        // using a valHook as follows:
        $.valHooks.textarea = {
            get: function (elem) {
                return elem.value.replace(/\r?\n/g, "\r\n");
            }
        };

        $('#createCSRSubmit').click(function (e) {
            if (!enableSubmit()) {
                window.alert("Invalid input");
                return;
            }
            e.preventDefault();
            console.log('createCSRSubmit');
            if (!window.confirm("Are you sure you want to submit certificate renewal request?")) {
                return;
            }
            console.log('after confirm');
            // User clicked ok so show the 'please wait' modal.
            $('#waitModal').modal('show');
            doProcessing();
            // Note, don't call $('#waitModal').modal('hide');  here, as it
            // will remove the modal before the doProcessing is invoked.
        });
    });


    function doProcessing() {
        const email = $('#emailInputText').val();
        const pw = $('#sign_up_password').val();
        const c = '${countryOID}'; // the OIDs are passed to jsp
        const o = '${orgNameOID}';
        const cn = '${cnOID}';
        const ou = '${ouOID}';
        const loc = '${locOID}';
        const messageElement = $("#responseMessage");

        let postTarget;
        let csrTextAreaVal;
        let dataPostEncodedVal;

        const token = $("meta[name='_csrf']").attr("content");
        const header = $("meta[name='_csrf_header']").attr("content");

        postTarget = $("#postLinkViaClient");
        const pem = createCSR(cn, ou, loc, o, c, pw);
        csrTextAreaVal = pem.privateKey + pem.csr;
        dataPostEncodedVal = $.param({csr: pem.csr}) +
            "&" + $.param({email: email});

        $.ajax({
            type: "POST", url: postTarget.attr("href"),
            data: dataPostEncodedVal,
            headers: {"X-CSRF-TOKEN": token},
            success: function (text) {
                if (text.substring(0, 7) === "SUCCESS") {
                    MvcUtil.showSuccessResponse('SUCCESS - Next steps:' +
                        '<ol>' +
                        '<li><b>Save private key file</b> (click Save Private Key As Text File or manually copy the highlighted text to a file).<br>' +
                        'You MUST keep this file safe - you will need this later on!</li>' +
                        '<li><b>You will be emailed</b> when your certificate is ready for download.</li>' +
                        '<li><b>Click Clear / Refresh</b> when done.</li>' +
                        '</ol>',
                        messageElement);
                    //$('#csrTextArea').show();
                    $('#csrTextArea').addClass("success");
                    const info =
                        'Save this file as a plain text file (not rich text with formatting).\n' +
                        'This file contains your encrypted private key and the RENEW certificate signing request (CSR).\n' +
                        'You MUST keep this file safe - you will need this later on. \n' +
                        'You will be emailed when your certificate is ready for download. \n' +
                        'Note, the private key is NOT sent to the server, ONLY you have this copy. \n' +
                        'CSR Subject Name: /C=' + c + '/O=' + o + '/OU=' + ou + '/L=' + loc + '/CN=' + cn + '\n\n';
                    $("#csrTextArea").text(info + '\n' + text + '\n' + csrTextAreaVal);
                    showDownloadLink(); //checks whether browser is supported and displays button accordingly
                    $("#createCSRSubmit").hide();
                    window.alert("Renew Submitted OK. On next page please Save the highlighted text to a plain text file:  \nEither copy from text area (Ctrl+A, Ctrl+C) or click save button.");
                } else {
                    MvcUtil.showErrorResponse(text, messageElement); //any other response than success or fail (does not count as success)
                }
            },
            error: function (xhr) {
                MvcUtil.showErrorResponse(xhr.responseText, messageElement);
            }
        }).always(function (response) {
            $('#waitModal').modal('hide');
            //scrolls to the bottom of main div (to certificate data)
            $('html, body').animate({
                scrollTop: $(document).height()
            }, 500);
        });
        //return true;
    }// end doProcessing()


</script>
</body>
</html>
