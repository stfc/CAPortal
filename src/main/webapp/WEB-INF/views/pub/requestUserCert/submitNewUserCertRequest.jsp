

<%--<%@ page session="false"%>--%>
<%@ taglib uri="http://java.sun.com/jsp/jstl/core" prefix="c" %>
<%@ taglib uri="http://java.sun.com/jsp/jstl/functions" prefix="fn" %>
<%@ taglib uri="http://www.springframework.org/tags" prefix="s" %>
<%@ taglib uri="http://www.springframework.org/tags/form" prefix="form" %>
<%@ taglib uri="http://www.springframework.org/security/tags" prefix="sec" %>

<!doctype html>

<html>

<head>

    <link rel="shortcut icon" href="${pageContext.request.contextPath}/resources/favicon.ico" type="image/x-icon"/>
    <title>Submit New User Certificate Request</title>
    <meta name="viewport" content="width=device-width, initial-scale=1.0"/>
    <meta name="description" content="Page for requesting new user certs for public."/>
    <meta name="author" content="David Meredith"/>
    <meta name="author" content="Sam Worley"/>
    <meta name="_csrf" content="${_csrf.token}"/>
    <!-- default header name is X-CSRF-TOKEN -->
    <meta name="_csrf_header" content="${_csrf.headerName}"/>
    <!-- Styles -->
    <%@ include file="../../../jspf/styles.jspf" %>
    <link href="${pageContext.request.contextPath}/resources/css/messages/messages.css" rel="stylesheet"/>
</head>

<body id="certBody">
<%@ include file="../../../jspf/header.jspf" %>
<div class="modal fade" id="helpModal" tabindex="-1" role="dialog" aria-labelledby="myModalLabel" aria-hidden="true">
    <div class="modal-dialog">
        <div class="modal-content">
            <div class="modal-header">
                <h4 class="modal-title" id="helpModalLabel">Requesting New User Certificate</h4>
            </div>
            <div class="modal-body">
                A user certificate is a digital token that can be used to authenticate you to different
                compute resources and web sites. Our certificates are internationally
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
                    <li><strong>An acknowledgement email</strong> is sent to you with details of your local/nearest
                        institution's
                        <abbr title="A person who will view your photo-ID and will approve your request">RA
                            operator</abbr></li>
                    <li><strong>Contact your local/nearest RA operator</strong> and take some phone-ID</li>
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
    <h2>Request New User Certificate <a data-bs-toggle="modal" data-bs-target="#helpModal"><i
            class="bi bi-question-circle"></i></a></h2>
    <ul>
        <li>When clicking 'Submit Request' a new <abbr title="Certificate Signing Request">CSR</abbr> is
            created by your <strong>browser</strong>.
        </li>
        <li><strong>ONLY the public key</strong> is sent to the server as a <abbr
                title="Certificate Signing Request">CSR</abbr>.
        </li>
        <li>The private key and password are <strong>NOT</strong> sent to the server.</li>
    </ul>
    <br/>
    <!--
    Define links that are used as the form action, one of these
    will be used depending on the server configuration.
    -->
    <a id="postLinkCsr" href="${pageContext.request.contextPath}/pub/requestUserCert/postCsr"
       style="display: none;"></a>
    <a id="postLinkCsrAttributes"
       href="${pageContext.request.contextPath}/pub/requestUserCert/postCsrAttributes"
       style="display: none;"></a>

    <form action="" class="form-horizontal" role="form">
        <input type="hidden" name="${_csrf.parameterName}" value="${_csrf.token}"/>
        <div class="form-group form-cols">
            <div class="col">
                <label for="cnInputText">Name</label>
            </div>
            <div class="col">
                <input type="text" id="cnInputText" class="form-control"
                       onChange="javascript:this.value=this.value.toLowerCase();" placeholder="firstname lastname"/>
                <div class="form-text">Full name will make up the Common Name of your certificate</div>
            </div>
        </div>
        <div class="form-group form-cols">
            <div class="col">
                <label for="raSelect">Your Institution</label>
            </div>
            <div class="col">
                <select id="raSelect" class="form-control">
                    <option selected disabled>Please Select...</option>
                    <c:forEach items="${ralistArray}" var="ra">
                        <option>${ra}</option>
                    </c:forEach>
                </select>
                <div class="form-text">After applying for your certificate, you will need to visit your local/nearest
                    institution RA and provide photo-ID
                </div>
            </div>
        </div>
        <div class="form-group form-cols">
            <div class="col">
                <label for="emailInputText">e-Mail</label>
            </div>
            <div class="col">
                <input type="text" id="emailInputText" class="form-control" placeholder="some@domain.com"/>
                <div class="form-text">You will receive an email to this address with the certificate ID/Serial number,
                    which you will need to download your signed certificate
                </div>
            </div>
        </div>
        <div class="form-group form-cols">
            <div class="col">
                <label for="pinInputText">PIN</label>
            </div>
            <div class="col">
                <input type="text" id="pinInputText" class="form-control" placeholder="memorable phrase"/>
                <div class="form-text">You will need to quote this to your RA to prove you submitted the certificate
                    request
                </div>
            </div>
        </div>
        <div class="form-group form-cols">
            <div class="col">
                <label for="sign_up_password">Key Password</label>
            </div>
            <div class="col">
                <input type="password" id="sign_up_password" class="form-control"/>
            </div>
        </div>
        <div class="form-group form-cols">
            <div class="col">
                <label for="sign_up_password_confirm">Confirm Password</label>
            </div>
            <div class="col">
                <input type="password" id="sign_up_password_confirm" class="form-control"/>

                <div class="form-text">The password is used to encrypt your locally generated private key</div>
            </div>
        </div>
            <div class="form-group form-cols">
                <div class="col">
                    <button id="createCSRSubmit" class="btn btn-sm btn-primary">Submit Request</button>
                    <div class="form-text">This may take some time depending on your browser/computer (it generates a
                        new public/private key-pair in your browser and sends the public key to the server)
                    </div>
                    <br />
                    <a id="refreshButton" class="btn btn-sm btn-info">Clear / Refresh</a>
                </div>
            </div>
    </form>
    <div id="responseMessage"></div>
    <a id="savetxt" class="btn btn-sm btn-primary">Save Private Key As Text File</a>
    <label for="csrTextArea"></label><textarea id="csrTextArea" style="width: 900px; height: 200px;" readonly
                                               aria-readonly="true"></textarea>
</div>

<!-- footer includes shared .js files -->
<%@ include file="../../../jspf/footer.jspf" %>
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
        if (element.val().match(/^[\sa-zA-Z0-9_ ]{10,50}$/) === null) {
            $(element).addClass("error").next().text("Invalid PIN (min 10 characters a to z 0 to 9)");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

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

    function cnValid() {
        const element = $("#cnInputText");
        if (element.val().length < 2 ||
            element.val().match(/^([\(\)\-0-9A-Za-z\s]*[\(\)\-0-9A-Za-z]{2,30}[\(\)\-0-9A-Za-z\s]*[\s][\(\)\-0-9A-Za-z\s]*[\(\)\-0-9A-Za-z]{2,30}[\(\)\-0-9A-Za-z\s]*)*$/) === null) {
            $(element).addClass("error").next().text("Invalid name (at least 2 names with min of 2 chars each)");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function raValid() {
        const element = $("#raSelect");
        if (element.val() === null) {
            $(element).addClass("error").next().text("Invalid RA selection");
            return false;
        } else {
            $(element).removeClass("error").next().text("");
            return true;
        }
    }

    function enableSubmit() {
        if (cnValid() && pwValid() && pwSame() && pinValid() && emailValid() && raValid()) {
            return true;
        } else {
            return false;
        }
    }

    function showDownloadLink() {
        $('#savetxt').show();
    }

    function doProcessing() {
        console.log('doProcessing');
        const pin = $('#pinInputText').val();
        const email = $('#emailInputText').val();
        const cn = $('#cnInputText').val().trim();
        const ra = $('#raSelect').val().trim();
        const pw = $('#sign_up_password').val();
        const ou_loc = ra.split(" ");
        console.log('ou_loc ' + ou_loc[0] + ' ' + ou_loc[1]);
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

        console.log('posting');
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
                        '<li><b>Contact your local RA</b> to prove your identity, find your local RA in \'RA List\' ' +
                        "<a id='goToRaPage2' href='${pageContext.request.contextPath}/pub/viewralist'>here</a></li>" +
                        '<li><b>Click Clear / Refresh</b> when done.</li>' +
                        '</ol>',
                        messageElement);
                    $('#csrTextArea').addClass("success");
                    const info =
                        'Save this file as a plain text file (not rich text with formatting).\n' +
                        'This file contains your encrypted private key and the certificate signing request (CSR).\n' +
                        'You MUST keep this file safe - you will need this later on. \n' +
                        'Note, the private key is NOT sent to the server, ONLY you have this copy. \n' +
                        'CSR Subject Name: /C=' + c + '/O=' + o + '/OU=' + ou + '/L=' + loc + '/CN=' + cn;
                    $("#csrTextArea").text(info + '\n' + text + '\n' + csrTextAreaVal);
                    showDownloadLink(); //checks f browser is supported and displays button accordingly
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
            return false;
        });
        //return true;
    }// end doProcessing()


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
            saveAs(textfile, $('#cnInputText').val().replace(/ /g, '') + "PrivateKeyAndCsr.txt");
        });
        $("#refreshButton").click(function () {
            location.reload(true); // force reload from server rather than cache
        });
        $("#raSelect").blur(function () {
            raValid();
        });
        $("#cnInputText").blur(function () {
            cnValid();
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
            console.log('createCSRSubmit');
            if (!window.confirm("Are you sure you want to submit this certificate request?")) {
                return;
            }
            console.log('after confirm');
            // User clicked ok so show the 'please wait' modal.
            $('#waitModal').modal('show');
            // Next delay the execution of the slow function with a timeout
            // which gives enough time for the dom to update.
            doProcessing();
            //setTimeout(function(){
            //     doProcessing();
            //   },
            //   1000);
            // Note, don't call $('#waitModal').modal('hide');  here, as it
            // will remove the modal before the doProcessing is invoked.
        });
    });
</script>
</body>
</html>
