function updateLoginButtonState() {
    let selectedEndpoint = $('#endpointSelect').find('option:selected').first();
    $('#loginButton').prop('disabled', !selectedEndpoint.val());
}

function resetLaunchState() {
    $('#endpointSelect').prop('disabled', false);
    updateLoginButtonState();
    $('html').css('cursor', 'default');
}

$(document).on('change', '#endpointSelect', function() {
    try {
        updateLoginButtonState();
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'health-records endpoint change');
        }
    }
});

function showError(errorMessage) {
    $('#errorMessage').text(errorMessage || 'An unexpected error occurred.');
    $('#messageContainer').removeClass('hidden');
}

function clearError() {
    $('#errorMessage').text('');
    $('#messageContainer').addClass('hidden');
}

function doWait(waiting) {
    $('#endpointSelect').prop('disabled', waiting);
    if (waiting) {
        $('#loginButton').prop('disabled', true);
    } else {
        updateLoginButtonState();
    }

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
        try {
            _callback();
        } catch (error) {
            if (typeof window.handleFrontEndException === 'function') {
                window.handleFrontEndException(error, 'health-records reportLaunch callback');
            }

            showError('Unable to start login for the selected health record provider.');
            doWait(false);
        }
    }).fail(function(jqXHR) {
        let message = jqXHR && jqXHR.responseText ? jqXHR.responseText : 'Unable to prepare login.';

        if (window.console && typeof window.console.error === 'function') {
            console.error("caught error reporting launch: '" + message + "' (status=" + (jqXHR ? jqXHR.status : 'unknown') + ")");
        }

        showError(message);
        doWait(false);
    });
}

$(document).on('click', '#loginButton', function() {
    try {
        clearError();
        doWait(true);

        let el = $('#endpointSelect').find('option:selected').first();
        let endpointId = el.val();

        if (!endpointId) {
            showError('Please select a health record provider.');
            doWait(false);
            return;
        }

        reportLaunch(endpointId, function() {
            let clientId = el.data('client-id');
            let scope = el.data('scope');
            let redirectUri = el.data('redirect-uri');
            let iss = el.data('iss');

            if (!window.FHIR || !FHIR.oauth2 || typeof FHIR.oauth2.authorize !== 'function') {
                throw new Error('FHIR OAuth client is not available.');
            }

            FHIR.oauth2.authorize({
                "client_id": clientId,
                "scope": scope,
                "redirect_uri": redirectUri,
                "iss": iss
            });
        });
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'health-records login click');
        }

        showError('Unable to start login for the selected health record provider.');
        doWait(false);
    }
});

$(document).ready(function() {
    try {
        resetLaunchState();
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'health-records document ready');
        }
    }
});

window.addEventListener('pageshow', function() {
    try {
        resetLaunchState();
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'health-records pageshow');
        }
    }
});