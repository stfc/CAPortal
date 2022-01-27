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
    const responseElementId = element.attr("id") + "Response";
    let responseElement = $("#" + responseElementId);
    if (responseElement.length === 0) {
        // responseElement.length = The number of elements currently matched.
        // responseElement does not exist, so insert a new responseElement after element.  
        if (type === 'success') {
            responseElement = $('<div id="' + responseElementId + '" class="' + type + '">' + text + '</div>').insertAfter(element);
        } else {
            responseElement = $('<span id="' + responseElementId + '" class="' + type + '" style="display:none">' + text + '</span>').insertAfter(element);
        }
    } else {
        // responseElement already exists, so replace it. 
        if (type === 'success') {
            responseElement.replaceWith('<div id="' + responseElementId + '" class="' + type + '">' + text + '</div>');
        } else {
            responseElement.replaceWith('<span id="' + responseElementId + '" class="' + type + '" style="display:none">' + text + '</span>');
        }
        responseElement = $("#" + responseElementId);
    }
    responseElement.fadeIn("slow");
};


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
