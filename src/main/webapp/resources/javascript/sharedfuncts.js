/* 
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

MvcUtil = {};
MvcUtil.showSuccessResponse = function(text, element) {
    if (text.substr(0, 7) === 'SUCCESS') {
        MvcUtil.showResponse("success", text, element);
    } else {
        MvcUtil.showResponse("error", text, element);
    }
};
MvcUtil.showErrorResponse = function showErrorResponse(text, element) {
    MvcUtil.showResponse("error", text, element);
};

MvcUtil.showResponse = function(type, text, element) {
    var responseElementId = element.attr("id") + "Response";
    var responseElement = $("#" + responseElementId);
    if (responseElement.length == 0) {
        // responseElement.length = The number of elements currently matched.
        // responseElement does not exist, so insert a new responseElement after element.  
        if (type == 'success') {
            responseElement = $('<div id="' + responseElementId + '" class="' + type + '">' + text + '</div>').insertAfter(element);
        } else {
            responseElement = $('<span id="' + responseElementId + '" class="' + type + '" style="display:none">' + text + '</span>').insertAfter(element);
        }
    } else {
        // responseElement already exists, so replace it. 
        if (type == 'success') {
            responseElement.replaceWith('<div id="' + responseElementId + '" class="' + type + '">' + text + '</div>');
        } else {
            responseElement.replaceWith('<span id="' + responseElementId + '" class="' + type + '" style="display:none">' + text + '</span>');
        }
        responseElement = $("#" + responseElementId);
    }
    responseElement.fadeIn("slow");
};

/*MvcUtil.xmlencode = function(xml) {
 //for IE 
 var text;
 if (window.ActiveXObject) {
 text = xml.xml;
 }
 // for Mozilla, Firefox, Opera, etc.
 else {
 text = (new XMLSerializer()).serializeToString(xml);
 }			
 return text.replace(/\&/g,'&'+'amp;').replace(/</g,'&'+'lt;')
 .replace(/>/g,'&'+'gt;').replace(/\'/g,'&'+'apos;').replace(/\"/g,'&'+'quot;');
 };*/

/**
 * If IE, returns a positive number to reflect version, and NaN for other browser like chrome,firefox
 * @returns number or NaN
 */
function ie_ver() {
    //See: http://stackoverflow.com/questions/17907445/how-to-detect-ie11
    msie = parseInt((/msie (\d+)/.exec(navigator.userAgent.toLowerCase()) || [])[1]);
    if (isNaN(msie)) {
        msie = parseInt((/trident\/.*; rv:(\d+)/.exec(navigator.userAgent.toLowerCase()) || [])[1]);
    }
    return msie;
}

function ieVersion() {
    //returns version of ie in use for flash download button
    //(feature detection for blob not enough)
    var ie = (function() {
        var undef, rv = -1; // Return value assumes failure.
        if (navigator.appName == 'Microsoft Internet Explorer') {
            var ua = navigator.userAgent;
            var re = new RegExp("MSIE ([0-9]{1,}[\.0-9]{0,})");
            if (re.exec(ua) != null)
                rv = parseFloat(RegExp.$1);
        }
        return ((rv > -1) ? rv : undef);
    }());
    return ie;
}

$(document).ready(function() {
    //checking for feature unsupported in ie < ie9 instead of browser sniffing
    if (!(document.addEventListener)) {
        $('.iesupport').show();
    }
    $('#certBody').on('change keyup keydown', 'input, textarea, select', function(e) {
        $(this).addClass('changed-input');
    });
    $(window).on('beforeunload', function() {
        if ($('.changed-input').length) {
            return 'Please ensure you have saved your certificate before navigating away.';
        }
    });
    //popover settings
    $(".dnPop").popover({
        trigger: 'hover'
    });
    
    //hides change email buttons
    $('#saveEmail').hide();
            $('#inputEmail').hide();
            $('#changeEmail').click(function() {
                $('#inputEmail').show();
                $('#changeEmail').hide();
                $('#saveEmail').show();
            });
});
