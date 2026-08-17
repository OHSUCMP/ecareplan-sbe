function renderCard(id, card) {
    return '<div class="row row-cols-1 row-cols-md-2 g-4">' +
        '<div class="col">' +
        '<div class="card h-100 shadow-sm">' +
        '<div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">' +
        '<h3 class="card-title mb-0">' + card.title + '</h3>' +
        '</div>' + // card-header
        '<div class="card-body">' +
        '<div class="row g-2 mb-3">' +
        renderCardRows(id, card.rows) +
        '</div>' + // row
        renderCardHistory(id, history) +
        '</div>' + // card-body
        '</div>' + // card
        '</div>' + // col
        '</div>'; // row
}

function renderCardRows(baseId, rows) {

}

function renderCardHistory(baseId, history) {
    if ( ! history ) return '';

    let html = '<div class="accordion accordion-flush border rounded" id="history-' + baseId + '">' +
        '<div class="accordion-item">' +
        '<h2 class="accordion-header" id="heading-' + baseId + '">' +
        '<button class="accordion-button collapsed py-2" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-' + baseId + '" aria-expanded="false" aria-controls="collapse-' + baseId + '">' +
        'View History (' + history.items.length + ' events)' +
        '</button>' +
        '</h2>' + // accordion-header
        '<div id="collapse-' + baseId + '" class="accordion-collapse collapse" aria-labelledby="heading-' + baseId + '" data-bs-parent="#history-' + baseId + '">' +
        '<div class="accordion-body">' +
        '<div class="row">' +
        '</div>' +
        // headers
        '<div class="row g-2 small">' +
        renderHistoryHeaders(baseId, history) +
        '</div>' + // row
        '</div>' + // accordion-body
        '</div>' // accordion-item

    let renderedHistoryItems = [];
    history.items.forEach((item) => {
        renderedHistoryItems.push(renderHistoryItem(baseId, item));
    });
    html += renderedHistoryItems.join(' ');
    html += '</div>'; // accordion

    return html;
}

function renderHistoryHeaders(baseId, history) {

}

function renderHistoryItem(baseId, historyItem) {

}

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
            console.log("dataset-update: dataSet=" + eventData.dataSet + ", endpoint=" + eventData.endpoint);
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
