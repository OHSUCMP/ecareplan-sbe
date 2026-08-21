$(document).on('change', '#endpointSelect', function() {
    let el = $(this).find('option:selected').first();
    $('#loginButton').prop('disabled', !el.val());
});

function showError(errorMessage) {
    $('#errorMessage').text(errorMessage);
    $('#messageContainer').removeClass('hidden');
}

function clearError() {
    $('#errorMessage').text('');
    $('#messageContainer').addClass('hidden');
}

function doWait(waiting) {
    $('#endpointSelect').prop('disabled', waiting);
    $('#loginButton').prop('disabled', waiting);
    let cursor = waiting ? 'wait' : 'default';
    $('html').css('cursor', cursor);
}

function reportLaunch(endpointId, _callback) {
    $.ajax({
        method: "POST",
        url: "/patient/health-records/report-launch",
        data: {
            endpointId: endpointId
        }
    }).done(function(data, textStatus, jqXHR) {
        _callback();
    }).fail(function(jqXHR) {
        console.error("caught error reporting launch: '" + jqXHR.responseText + "' (status=" + jqXHR.status + ")");
        showError(jqXHR.responseText);
        doWait(false);
    });
}

$(document).on('click', '#loginButton', function() {
    doWait(true);

    let el = $('#endpointSelect').find('option:selected').first();
    let endpointId = el.val();

    reportLaunch(endpointId, function() {
        let clientId = el.data('client-id');
        let scope = el.data('scope');
        let redirectUri = el.data('redirect-uri');
        let iss = el.data('iss');

        FHIR.oauth2.authorize({
            "client_id": clientId,
            "scope": scope,
            "redirect_uri": redirectUri,
            "iss": iss
        });
    });
});
