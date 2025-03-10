<%-- <%@page import="net.tanesha.recaptcha.ReCaptchaFactory"%>
<%@page import="net.tanesha.recaptcha.ReCaptcha"%> --%>

<%@ page session="false" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<!doctype html>

<html>

<head>

    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Request Host Certificate</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Page for requesting new host certs for cert owners."/>
    <meta name="author" content="David Meredith"/>
    <meta name="author" content="Sam Worley"/>
    <meta name="_csrf" content="${_csrf.token}"/>
    <!-- default header name is X-CSRF-TOKEN -->
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
    <!-- Styles -->
    <%@ include file="../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body id="certBody">
<%@ include file="../../jspf/header.jspf" %>
<div class="modal fade" id="helpModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel"
     aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content"></div>
    </div>
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title" id="helpModalLabel">Requesting New Host Certificate</h4>
            </div>
            <div class="modal-body">
                A host certificate is a digital token that can be used to authenticate your resources/servers to
                different
                compute resources. Our certificates are internationally
                <abbr title="Trusted by the International Grid Trust Federation - IGTF">trusted</abbr> by
                different resource providers.
                <br/>
                <br/>
                The certificate request process:
                <ol>
                    <li><strong>Complete the form</strong> (note, you can hover over each form title for info)</li>
                    <li><strong>Click 'Submit Request'</strong></li>
                    <li><strong>Save the highlighted text</strong> to a local text file, either: <br/>
                        <ul>
                            <li>Click the 'Save' button (this may not appear in some older browsers)</li>
                            <li>Or manually copy the text from the text-area <br/>(Ctrl+A then Ctrl+C then paste into a
                                .txt file)
                            </li>
                        </ul>
                    </li>
                    <li><strong>Keep this text file safe</strong> and do not modify it!
                        it contains your certificate's private key and only you have a copy
                    </li>
                    <li><strong>An acknowledgement email</strong> is sent to you</li>
                    <li><strong>Download your certificate</strong> after you have been e-mailed your certificate serial
                        number (see 'Certificates | Download' page)
                    </li>
                </ol>
            </div>
            <div class="modal-footer">
                Click anywhere off this panel to close
            </div>
        </div>
    </div>
</div>

<!-- Wrap all page content here -->
<div id="wrap" class="container">
    <div class="row">
        <div class="col-offset-1">
            <div class="row">
                <div class="col-10"><h2>Request New Host Certificate <a data-bs-toggle="modal" data-bs-target="#helpModal"><i class="bi bi-question-circle"></i></a></h2></div>
            </div>
            <ul>
                <li>When clicking 'Submit Request' a new <abbr title="Certificate Signing Request">CSR</abbr> is
                    created by your <strong>browser</strong>.
                </li>
                <li><strong>ONLY the public key</strong> is sent to the server as a <abbr
                        title="Certificate Signing Request">CSR</abbr>.
                </li>
                <li>The private key and password are <strong>NOT</strong> sent to the server.</li>
            </ul>

            <c:if test="${not empty message}">
                <div id="message" class="success">${message}</div>
            </c:if>
            <c:if test="${status.error}">
                <div id="message" class="error">Form has errors</div>
            </c:if>
            <br/>
            <h4>Requestor Identity: ${cert.subjectDN}</h4>
            <br/>
            <c:if test="${hostCert == true}">
                <div class="error">
                    <ul>
                        <li>Your selected browser certificate is a host certificate.</li>
                        <li>You need a VALID <b>user</b> certificate to request a new Host Certificate.</li>
                        <li>Please clear your browser ssl cache, restart your browser and select a VALID user
                            certificate.
                        </li>
                    </ul>
                </div>
            </c:if>

            <c:if test="${hostCert == false}">
                <a id="postLinkCsr" href="${pageContext.request.contextPath}/cert_owner/requestHostCert/postCsr"
                   style="display: none;"></a>
                <a id="postLinkCsrAttributes"
                   href="${pageContext.request.contextPath}/cert_owner/requestHostCert/postCsrAttributes"
                   style="display: none;"></a>

                <form action="" class="form-horizontal" role="form">
                    <div class="form-group form-cols">
                        <div class="col">
                                <label for="cnInputText">Hostname</label>
                        </div>
                        <div class="col">
                            <input type="text" id="cnInputText" class="form-control" placeholder="some.dnsname.ac.uk"
                                   onChange="javascript:this.value=this.value.toLowerCase();"/>
                            <span></span>
                            <div class="form-text">Hostname will make up the Common Name of your host certificate</div>
                        </div>
                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="raSelect">Your RA</label>
                            <span class="muted">(Registration Authority)</span>
                        </div>
                        <div class="col">
                            <select id="raSelect" class="form-control">
                                <option selected disabled>Please Select, your RA is shown first below for
                                    convenience...
                                </option>
                                <c:forEach items="${ralistArray}" var="ra">
                                    <option>${ra}</option>
                                </c:forEach>
                            </select><span></span>
                            <div class="form-text">The RA defines the certificate Locality and Org name</div>
                        </div>
                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="emailInputText">e-Mail</label>
                        </div>
                        <div class="col">
                            <input type="text" id="emailInputText" class="form-control"
                                   placeholder="some@domain.com"/><span></span>
                            <div class="form-text">You will receive an email to this address with the certificate ID/Serial number, which you will need to download your signed certfiicate</div>
                        </div>

                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="pinInputText">PIN</label>
                        </div>
                        <div class="col">
                            <input type="text" id="pinInputText" class="form-control"
                                   placeholder="memorable phrase"/><span></span>
                            <div class="form-text">For host cert requests, you may need to quote this to your RA to prove you submitted the CSR (usually not required)</div>
                        </div>
                    </div>
                    <div class="form-group form-cols">
                        <div class="col">
                            <label for="sign_up_password">Key Password</label>
                        </div>
                        <div class="col">
                            <input type="password" id="sign_up_password" class="form-control"/><span></span>
                            <div class="form-text">The password is used to encrypt your locally generated private key</div>
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
                            <a id="createCSRSubmit" class="btn btn-sm btn-primary">
                                Submit Request
                            </a><br />
                            <div class="form-text">This may take some time depending on your browser/computer
                                (it generates a new public/private key-pair in the browser and sends the public key to the server)</div>
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
                <textarea id="csrTextArea" style="width: 900px; height: 200px;" readonly aria-readonly="true"></textarea>
            </div>
        </div>
    </div>
</div>

<%@ include file="../../jspf/footer.jspf" %>
<!-- Stuff for crypto / csrs -->
<script src="https://cdn.jsdelivr.net/npm/node-forge@1.3.1/dist/forge.min.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/base64.js"></script>
<!-- https://github.com/eligrey/FileSaver.js -->
<script src="${pageContext.request.contextPath}/resources/javascript/FileSaver.js"></script>
<script src="${pageContext.request.contextPath}/resources/javascript/crypto.js"></script>

<script type="text/javascript">

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
            $(element).addClass("error").next().text("Password too short (min 10 characters)");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function pinValid() {
        const element = $("#pinInputText");
        if (element.val().match(/^[\sa-zA-Z0-9_]{10,50}$/) === null) {
            $(element).addClass("error").next().text("Invalid PIN (min 10 characters a to z 0 to 9)");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function emailValid() {
        const element = $("#emailInputText");
        //if (element.val().match(/^(([0-9a-zA-Z]+[\-\._])*[0-9a-zA-Z]+@([-0-9a-zA-Z]+[.])+[a-zA-Z]{2,6}[,;]?)+$/) === null) {
        if (element.val().match(/^[_A-Za-z0-9-\+]+(\.[_A-Za-z0-9-]+)*@[A-Za-z0-9-]+(\.[A-Za-z0-9]+)*(\.[A-Za-z]{2,})$/) === null) {
            $(element).addClass("error").next().text("Invalid email");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function cnValid() {
        //if(true) return true;
        const element = $("#cnInputText");
//              if(element.val().match(/^([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9])(\.([a-zA-Z0-9]|[a-zA-Z0-9][a-zA-Z0-9\-]{0,61}[a-zA-Z0-9]))*$/) === null){
        if (element.val().match(/^([a-z]{1,30}\/)?(?=.{1,254}$)((?=[a-z0-9-]{1,63}\.)(xn--)?[a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,63}$/i) === null) {
            //if(element.val().match(/^(?=.{1,254}$)((?=[a-z0-9-]{1,63}\.)(xn--)?[a-z0-9]+(-[a-z0-9]+)*\.)+[a-z]{2,63}$/i) === null){
            $(element).addClass("error").next().text("Invalid host name (only a-z 0-9 - . accepted)");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function raValid() {
        const element = $("#raSelect");
        if ($(element).val() === null) {
            $(element).addClass("error").next().text("Invalid RA selection");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }


    function enableSubmit() {
        if (pwValid() && pwSame() && pinValid() && emailValid() && cnValid() && raValid()) {
            return true;
        } else {
            return false;
        }
    }

    function showDownloadLink() {
        $('#savetxt').show();  // isFileSaverSupported is true
    }

    function doProcessing() {
        const ra = $('#raSelect').val();
        const cn = $('#cnInputText').val().trim();
        const email = $('#emailInputText').val();
        const pw = $('#sign_up_password').val();
        const pin = $('#pinInputText').val();
        const ou_loc = ra.split(" ");
        const ou = ou_loc[0];
        const loc = ou_loc[1];
        const c = '${countryOID}';
        const o = '${orgNameOID}';
        const messageElement = $("#responseMessage");

        let postTarget;
        let csrTextAreaVal;
        let dataPostEncodedVal;
        const token = $("meta[name='_csrf']").attr("content");
        const header = $("meta[name='_csrf_header']").attr("content");

        postTarget = $("#postLinkCsr");
        const pem = createCSR(cn, ou, loc, o, c, pw);
        csrTextAreaVal = pem.privateKey + pem.csr;
        dataPostEncodedVal =
            $.param({pin: pin}) +
            "&" + $.param({email: email}) +
            "&" + $.param({csr: pem.csr});

        $.ajax({
            type: "POST", url: postTarget.attr("href"),
            data: dataPostEncodedVal,
            headers: {"X-CSRF-TOKEN": token},
            success: function (text) {
                if (text.substring(0, 7) === "SUCCESS") {
                    MvcUtil.showSuccessResponse('SUCCESS - Next steps:' +
                        '<ol>' +
                        '<li><b>Save private key file</b> (Copy the highlighted text to a file or click save button).<br>' +
                        'You MUST keep this file safe - you will need this later on!</li>' +
                        '<li><b>Click Clear / Refresh</b> when done.</li>' +
                        '</ol>',
                        messageElement);
                    $('#csrTextArea').addClass("success");
                    //$('#doneButton').show();
                    const info =
                        'Save this file as a plain text file (not rich text with formatting).\n' +
                        'This file contains your encrypted private key and the certificate signing request (CSR).\n' +
                        'You MUST keep this file safe - you will need this later on. \n' +
                        'Note, the private key is NOT sent to the server, ONLY you have this copy. \n' +
                        'CSR Subject Name: /C=' + c + '/O=' + o + '/OU=' + ou + '/L=' + loc + '/CN=' + cn;
                    $("#csrTextArea").text(info + '\n' + text + '\n' + csrTextAreaVal);
                    showDownloadLink(); //checks whether browser is supported and displays button accordingly
                    $('#csrTextArea').show();
                    $("#createCSRSubmit").hide();
                    window.alert("Submitted OK. On next page please Save the highlighted text to a plain text file:  \nEither copy from text area (Ctrl+A, Ctrl+C) or click save button.");
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
    }

    $(document).ready(function () {
        $("#createCSRSubmit").tooltip();
        $("#savetxt").tooltip();
        $("#cnInputTextTitle").tooltip();
        $("#emailInputTextTitle").tooltip();
        $("#raSelectTitle").tooltip();
        $("#pinInputTextTitle").tooltip();
        $("#sign_up_passwordTitle").tooltip();
        $("#sign_up_password_confirmTitle").tooltip();

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

        $('#savetxt').click(function () {
            const textfile = new Blob([$('#csrTextArea').val()], {type: "text/plain;charset=utf-8"});
            saveAs(textfile, $('#cnInputText').val().replace(/[.]/g, '_') + "PrivateKeyAndCsr.txt");
        });
        $("#refreshButton").click(function () {
            location.reload(true); // force reload from server rather than cache
        });

        $("#cnInputText").blur(function () {
            cnValid();
        });
        $("#raSelect").blur(function () {
            raValid();
        });
        $("#emailInputText").blur(function () {
            emailValid();
        });
        $("#sign_up_password").blur(function () {
            pwValid();
            pwSame();
        });
        $("#sign_up_password_confirm").blur(function () {
            pwValid();
            pwSame();
        });
        $("#pinInputText").blur(function () {
            pinValid();
        });
        $("#cnInputText").keyup(function () {
            cnValid();
        });
        $("#emailInputText").keyup(function () {
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
        $("#pinInputText").keyup(function () {
            pinValid();
        });
        $("#helpMod").click(function () {
            $('#helpModal').modal('show');
        });
        $("#createCSRSubmit").click(function (e) {
            if (!enableSubmit()) {
                window.alert("Invalid input");
                return;
            }
            e.preventDefault();
            if (!window.confirm("Are you sure you want to submit this certificate request?")) {
                return;
            }
            // User clicked ok so show the 'please wait' modal.
            $('#waitModal').modal('show');
            // Next delay the execution of the slow function with a timeout
            // which gives enough time for the dom to update.
            setTimeout(function () {
                    doProcessing();
                },
                1000);
            // Note, don't call $('#waitModal').modal('hide');  here, as it
            // will remove the modal before the doProcessing is invoked.
        });
    });
</script>
</body>
</html>
