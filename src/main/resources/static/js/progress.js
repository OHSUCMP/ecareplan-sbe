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

function isAnyProgressRunning(progressData) {
    const runningStatuses = new Set(['INITIALIZING', 'RUNNING']);
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
        html += '<div>' + item.label + ': ' + item.message + ' (' + item.percentComplete + '%)</div>';
        if (item.errors && item.errors.length > 0) {
            html += '<div>Errors:<ul>';
            $.each(item.errors, function(j, error) {
                html += '<li>' + error + '</li>';
            })
            html += '</ul></div>';
        }
        html += '</div>';
    });
    return html;
}

function getCurrentProgress(_callback) {
    $.ajax({
        method: "POST",
        url: "/current-progress"
    }).done(function(progressData) {
        _callback(progressData);
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
