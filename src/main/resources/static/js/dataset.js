function refreshProgress() {
    getCurrentProgress(function(progressData) {
        if (progressData) {
            let el = $('#progressContainer');
            $(el).html(renderProgressData(progressData));
            $(el).removeClass('hidden');

            if (isAnyProgressRunning(progressData)) {
                setTimeout(refreshProgress, 5000);

            } else if (isAllProgressComplete(progressData)) {
                setTimeout(clearProgress, 30000);
            }
        }
    });
}

function getCurrentProgress(_callback) {
    $.ajax({
        method: "POST",
        url: getBasePath() + '/progress'
    }).done(function(progressData) {
        _callback(progressData);
    });
}

function isAnyProgressRunning(progressData) {
    const runningStatuses = new Set(['WAITING_TO_START', 'RUNNING']);
    return Array.isArray(progressData)
        && progressData.some(item => runningStatuses.has(item.status));
}

function isAllProgressComplete(progressData) {
    return Array.isArray(progressData)
        && progressData.length > 0
        && progressData.every(item => item.status === 'COMPLETED');
}

function clearProgress() {
    let el = $('#progressContainer');
    $(el).html('');
    $(el).addClass('hidden');
}

function renderProgressData(progressData) {
    // console.log("ProgressData: " + JSON.stringify(progressData));
    let html = '';
    $.each(progressData, function(i, item) {
        html += '<div class="progress">';
        html += '<div>' + item.label + ': ' + item.message;
        if (item.percentComplete !== null) {
            html += ' (' + item.percentComplete + '%)';
        }
        html += '</div>';
        if (item.errors && item.errors.length > 0) {
            html += '<br/>Errors:<ul>';
            $.each(item.errors, function(j, error) {
                html += '<li>' + error + '</li>';
            });
            html += '</ul>';
        }
        html += '</div>';
    });
    return html;
}

function getDataSets() {
    return $('#dataSets').html().split(',').map(s => s.trim());
}

function initializeSSE() {
    const eventSource = new EventSource(getBasePath() + '/sse');

    eventSource.addEventListener("dataset-update", function(event) {
        const eventData = JSON.parse(event.data);
        if (getDataSets().includes(eventData.dataSet)) {
            console.log("dataset-update: dataSet=" + eventData.dataSet + ", endpointId=" + eventData.endpointId);
            getUpdatedModels(renderModels);
        }
    });
}

function getUpdatedModels(_callback) {
    $.ajax({
        method: "POST",
        url: getBasePath() + '/models'
    }).done(function(updatedModels) {
        _callback(updatedModels);
    });
}

$(document).ready(function() {
    if (!exists('#sessionEstablished')) {
        return;
    }
    if ($('#sessionEstablished').text() !== 'true') {
        return;
    }
    refreshProgress();
});
