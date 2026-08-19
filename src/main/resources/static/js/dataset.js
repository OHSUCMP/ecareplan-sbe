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
        renderCardHistory(id, card.history) +
        '</div>' + // card-body
        '</div>' + // card
        '</div>' + // col
        '</div>'; // row
}

function renderCardRows(baseId, rows) {
    if (!rows || rows.length === 0) return '';

    let html = '';

    rows.forEach(function(row, rowIndex) {
        if (!row || row.length === 0) return;

        html += '<div class="col-12" id="' + baseId + '-row-' + rowIndex + '">';
        html += '<div class="row g-2">';

        row.forEach(function(cell, cellIndex) {
            html += '<div class="col" id="' + baseId + '-row-' + rowIndex + '-cell-' + cellIndex + '">';
            html += '<div class="border rounded bg-light p-2 h-100">';
            html += cell ?? '';
            html += '</div>';
            html += '</div>';
        });

        html += '</div>';
        html += '</div>';
    });

    return html;
}

function renderCardHistory(baseId, history) {
    if (!history || !history.rows || history.rows.length === 0) return '';

    // todo : construct a bootstrap-based grid based on the data in the history parameter.
    //        the history parameter will have an element 'headers' which is an array of strings, and should be
    //        displayed at the top of the grid, one item per cell, and formatted appropriately in the context of
    //        a table header that will be followed by one or more rows of data.
    //

    return '<div class="accordion accordion-flush border rounded" id="history-' + baseId + '">' +
        '<div class="accordion-item">' +
        '<h2 class="accordion-header" id="heading-' + baseId + '">' +
        '<button class="accordion-button collapsed py-2" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-' + baseId + '" aria-expanded="false" aria-controls="collapse-' + baseId + '">' +
        'View History (' + history.rows.length + ' events)' +
        '</button>' +
        '</h2>' + // accordion-header
        '<div id="collapse-' + baseId + '" class="accordion-collapse collapse" aria-labelledby="heading-' + baseId + '" data-bs-parent="#history-' + baseId + '">' +
        '<div class="accordion-body">' +
        '<div class="table-responsive">' +
        '<table class="table table-sm table-striped table-hover align-middle mb-0">' +
        '<thead>' +
        '<tr>' +
        renderHistoryHeaders(baseId, history.headers) +
        '</tr>' +
        '</thead>' +
        '<tbody>' +
        renderHistoryRows(baseId, history.rows) +
        '</tbody>' +
        '</table>' +
        '</div>' + // table-responsive
        '</div>' + // accordion-body
        '</div>' + // accordion-collapse
        '</div>' + // accordion-item
        '</div>'; // accordion
}

function renderHistoryHeaders(baseId, headers) {
    // todo : render history headers

    if (!headers || headers.length === 0) return '';

    let html = '';

    headers.forEach(function(header, index) {
        html += '<th scope="col" class="text-nowrap" id="' + baseId + '-history-header-' + index + '">';
        html += header ?? '';
        html += '</th>';
    });

    return html;
}

function renderHistoryRows(baseId, rows) {
    // todo : render history rows

    if (!rows || rows.length === 0) return '';

    let html = '';

    rows.forEach(function(row, rowIndex) {
        if (!row || row.length === 0) return;

        html += '<tr id="' + baseId + '-history-row-' + rowIndex + '">';

        row.forEach(function(cell, cellIndex) {
            html += '<td id="' + baseId + '-history-row-' + rowIndex + '-cell-' + cellIndex + '">';
            html += cell ?? '';
            html += '</td>';
        });

        html += '</tr>';
    });

    return html;
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

function initializeSSE(_callback) {
    const eventSource = new EventSource(getBasePath() + '/sse');

    eventSource.addEventListener("dataset-update", function(event) {
        const eventData = JSON.parse(event.data);
        if (getDataSets().includes(eventData.dataSet)) {
            console.log("dataset-update: dataSet=" + eventData.dataSet + ", endpoint=" + eventData.endpoint);
            getUpdatedModels(_callback);
        }
    });
}

function getUpdatedModels(_callback) {
    $.ajax({
        method: "POST",
        url: getBasePath() + '/models'
    }).done(function(models) {
        _callback(models);
    });
}

function renderModels(models) {
    if (models && models.length > 0) {
        let cards = [];
        let id = 1;
        models.forEach(function(model) {
            cards.push(renderCard('card_' + id, buildCardData(model)));
            id ++;
        });
        $('#modelsContainer').html(cards.join('\n'));
    }
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
