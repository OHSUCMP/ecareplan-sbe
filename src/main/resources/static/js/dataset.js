function renderCardSelectors(id, cardSelector, card, selected) {
    let html = '<button type="button" class="list-group-item list-group-item-action card-selector' +
        (selected ? ' active' : '') + '"' +
        ' id="' + id + '-selector" data-card-target="' + id + '-panel"' +
        ' aria-controls="' + id + '-panel" aria-selected="' + (selected ? 'true' : 'false') + '">';

    if (Array.isArray(cardSelector)) {
        html += '<div class="card-selector-fields d-flex flex-column flex-md-row gap-2 w-100">';
        cardSelector.forEach(function(value, index) {
            html += '<div class="' + (index === 0 ? 'fw-semibold ' : '') + 'flex-grow-1">' + (value ?? '') + '</div>';
        });
        html += '</div>';
    } else {
        html += cardSelector ?? '';
    }

    html += '</button>';
    return html;
}

function renderCardSelectorLayout(items) {
    let selectors = [];
    let cards = [];

    items.forEach(function(item, index) {
        let id = 'card_' + (index + 1);
        selectors.push(renderCardSelectors(id, item.selector, item.card, index === 0));
        cards.push('<div class="card-panel' + (index === 0 ? '' : ' d-none') + '"' +
            ' id="' + id + '-panel" role="tabpanel" aria-labelledby="' + id + '-selector"' +
            (index === 0 ? '' : ' aria-hidden="true"') + '>' +
            renderCard(id, item.card) +
            '</div>');
    });

    return '<div class="row g-4 card-selector-layout">' +
        '<div class="col-6 col-lg-6">' +
        '<div class="card-selector-scroll">' +
        '<div class="list-group" role="tablist" aria-label="Available records">' +
        selectors.join('\n') +
        '</div>' +
        '</div>' +
        '</div>' +
        '<div class="col-6 col-lg-6">' +
        cards.join('\n') +
        '</div>' +
        '</div>';
}

function resizeCardSelectorList() {
    $('.card-selector-scroll').each(function() {
        let top = this.getBoundingClientRect().top;
        this.style.height = Math.max(0, window.innerHeight - top - 50) + 'px';
    });
}

function renderCard(id, card) {
    return '<div class="row row-cols-1 row-cols-md-1 g-4">' +
        '<div class="col">' +
        '<div class="card h-100 shadow-sm">' +
        '<div class="card-header bg-primary text-white d-flex justify-content-between align-items-center">' +
        '<h3 class="card-title mb-0">' + card.title +

        // todo : if it exists, card.learnMoreUrl should appear in the same header bar as card.title, but
        //        right-aligned, and in smaller text.  it should be a hyperlink with the text "Learn More" that directs
        //        to the specified URL

        '</h3>' +
        '</div>' + // card-header
        '<div class="card-body">' +
        (typeof renderCardChartContainer === 'function' ? renderCardChartContainer(id, card.chart) : '') +
        renderCardRows(id, card.rows) +
        renderCardHistory(id, card.history) +
        '</div>' + // card-body
        '</div>' + // card
        '</div>' + // col
        '</div>'; // row
}

function renderCardRows(baseId, rows) {
    if (!rows || rows.length === 0) return '';

    let html = '<div class="row g-2 mb-3">';

    rows.forEach(function (row, rowIndex) {
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

    html += '</div>'; // row

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
        if (typeof buildCardSelectorData === 'function') {
            let items = [];
            models.forEach(function(model) {
                items.push({
                    selector: buildCardSelectorData(model),
                    card: buildCardData(model)
                });
            });

            $('#modelsContainer').html(renderCardSelectorLayout(items));
            resizeCardSelectorList();

        } else {
            let cards = [];
            let i = 1;
            models.forEach(function(model) {
                cards.push(renderCard('card_' + i, buildCardData(model)));
                i ++;
            });

            $('#modelsContainer').html(cards.join('\n'));
        }
    }

    if (typeof renderCharts === 'function') {
        renderCharts();
    }
}

$(document).on('click', '#modelsContainer .card-selector', function() {
    let selector = $(this);
    let targetId = selector.attr('data-card-target');
    let container = $('#modelsContainer');

    container.find('.card-selector').removeClass('active').attr('aria-selected', 'false');
    selector.addClass('active').attr('aria-selected', 'true');
    container.find('.card-panel').addClass('d-none').attr('aria-hidden', 'true');
    container.find('#' + targetId).removeClass('d-none').removeAttr('aria-hidden');
});

$(window).on('resize', resizeCardSelectorList);

$(document).ready(function() {
    if (!exists('#sessionEstablished')) {
        return;
    }
    if ($('#sessionEstablished').text() !== 'true') {
        return;
    }

    getCurrentProgress(function(progressData) {
        if (progressData) {
            if ( ! isAllProgressComplete(progressData) ) {
                let el = $('#progressContainer');
                $(el).html(renderProgressData(progressData));
                $(el).removeClass('hidden');
                setTimeout(refreshProgress, 5000);
            }
        }
    });
});
