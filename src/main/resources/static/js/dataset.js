function getCardSelectorSortValue(value) {
    return $('<div>').html(value ?? '').text().trim().toLowerCase();
}

function renderCardSelectors(id, cardSelector, card, selected) {
    let sortAttributes = '';

    if (Array.isArray(cardSelector)) {
        cardSelector.forEach(function(selectorItem, index) {
            let valueForSort = selectorItem.valueForCompare !== undefined && selectorItem.valueForCompare !== null ?
                selectorItem.valueForCompare :
                selectorItem.value;

            sortAttributes += ' data-sort-value-' + index + '="' + escapeHtml(getCardSelectorSortValue(valueForSort)) + '"';
        });
    }

    let html = '<button type="button" class="list-group-item list-group-item-action card-selector' +
        (selected ? ' active' : '') + '"' +
        ' id="' + id + '-selector" data-card-target="' + id + '-panel"' +
        ' aria-controls="' + id + '-panel" aria-selected="' + (selected ? 'true' : 'false') + '"' +
        sortAttributes + '>';

    if (Array.isArray(cardSelector)) {
        html += '<div class="row g-4 row-cols-sm-' + cardSelector.length + ' g-4 card-selector-fields">';
        cardSelector.forEach(function(selectorItem) {
            html += '<div class="col-sm-' + selectorItem.bootstrapWidth + ' card-selector-field">' +
                (selectorItem.value ?? '') +
                '</div>';
        });
        html += '</div>';
    } else {
        html += cardSelector ?? '';
    }

    html += '</button>';
    return html;
}

function renderCardSelectorHeaders(selector) {
    if (!Array.isArray(selector) || selector.length === 0) {
        return '';
    }

    let html = '<div class="list-group-item card-selector-header">' +
        '<div class="row g-4 row-cols-sm-' + selector.length + ' g-4 card-selector-fields">';

    selector.forEach(function(selectorItem, index) {
        html += '<div class="col-sm-' + selectorItem.bootstrapWidth + ' card-selector-field">' +
            '<button type="button" class="card-selector-sort-button" data-sort-index="' + index + '" aria-sort="none">' +
            '<span class="card-selector-sort-label">' +
            '<span class="card-selector-sort-text">' + escapeHtml(selectorItem.label ?? '') + '</span>' +
            '</span>' +
            '<span class="card-selector-sort-indicator" aria-hidden="true"></span>' +
            '</button>' +
            '</div>';
    });

    html += '</div>' +
        '</div>';

    return html;
}

function renderDatasetFilter(query = '') {
    return '<div class="dataset-filter">' +
        '<label class="form-label" for="datasetFilter">Filter records</label>' +
        '<input class="form-control" id="datasetFilter" type="search"' +
        ' value="' + escapeHtml(query) + '"' +
        ' placeholder="Type to filter these records" autocomplete="off"' +
        ' aria-describedby="datasetFilterStatus">' +
        '<div class="dataset-filter-status" id="datasetFilterStatus" role="status" aria-live="polite"></div>' +
        '</div>';
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

    return renderDatasetFilter(datasetFilterQuery) +
        '<div class="row g-4 card-selector-layout">' +
        '<div class="col-6 d-none d-md-block">' +
        '<div class="card-selector-list-container">' +
        renderCardSelectorHeaders(items[0]?.selector) +
        '<div class="card-selector-scroll">' +
        '<div class="list-group card-selector-items" role="tablist" aria-label="Available records">' +
        selectors.join('\n') +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '<div class="col-12 col-md-6 card-selector-card-list">' +
        cards.join('\n') +
        '</div>' +
        '</div>';
}

let datasetFilterQuery = '';

function selectFirstVisibleCard(layout) {
    let selector = layout.find('.card-selector:not(.dataset-filtered-out)').first();

    layout.find('.card-selector').removeClass('active').attr('aria-selected', 'false');
    layout.find('.card-panel').addClass('d-none').attr('aria-hidden', 'true');

    if (selector.length === 0) {
        return;
    }

    selector.addClass('active').attr('aria-selected', 'true');
    layout.find('#' + selector.attr('data-card-target'))
        .removeClass('d-none')
        .removeAttr('aria-hidden');
}

function clearDatasetFilterHighlights(container) {
    container.find('mark.dataset-filter-highlight').each(function() {
        let highlight = $(this);
        highlight.replaceWith(document.createTextNode(highlight.text()));
    });

    container.find('.card-panel').each(function() {
        this.normalize();
    });
}

function highlightDatasetFilterText(element, query) {
    if (!query) {
        return;
    }

    let normalizedQuery = query.toLocaleLowerCase();

    $(element).contents().each(function() {
        if (this.nodeType === Node.TEXT_NODE) {
            let text = this.nodeValue;
            let normalizedText = text.toLocaleLowerCase();
            let matchIndex = normalizedText.indexOf(normalizedQuery);

            if (matchIndex === -1) {
                return;
            }

            let fragment = document.createDocumentFragment();
            let currentIndex = 0;

            while (matchIndex !== -1) {
                if (matchIndex > currentIndex) {
                    fragment.appendChild(document.createTextNode(text.substring(currentIndex, matchIndex)));
                }

                let mark = document.createElement('mark');
                mark.className = 'dataset-filter-highlight';
                mark.textContent = text.substring(matchIndex, matchIndex + query.length);
                fragment.appendChild(mark);

                currentIndex = matchIndex + query.length;
                matchIndex = normalizedText.indexOf(normalizedQuery, currentIndex);
            }

            if (currentIndex < text.length) {
                fragment.appendChild(document.createTextNode(text.substring(currentIndex)));
            }

            this.parentNode.replaceChild(fragment, this);

        } else if (this.nodeType === Node.ELEMENT_NODE) {
            let child = $(this);

            if (!child.is('script, style, mark')) {
                highlightDatasetFilterText(this, query);
            }
        }
    });
}

function applyDatasetFilter() {
    let container = $('#modelsContainer');
    let layout = container.find('.card-selector-layout');
    let normalizedQuery = datasetFilterQuery.trim().toLocaleLowerCase();
    let query = datasetFilterQuery.trim();
    let selectors = layout.find('.card-selector');
    let matchingCount = 0;

    clearDatasetFilterHighlights(container);

    selectors.each(function() {
        let selector = $(this);
        let panel = layout.find('#' + selector.attr('data-card-target'));
        let matches = normalizedQuery === '' || panel.text().toLocaleLowerCase().includes(normalizedQuery);

        selector.toggleClass('dataset-filtered-out', !matches);
        panel.toggleClass('dataset-filtered-out', !matches);
        if (matches) {
            matchingCount++;

            if (query !== '') {
                highlightDatasetFilterText(panel, query);
            }
        }
    });

    let selectedSelector = layout.find('.card-selector.active');
    if (selectedSelector.length === 0 || selectedSelector.hasClass('dataset-filtered-out')) {
        selectFirstVisibleCard(layout);
    }

    let status = matchingCount === selectors.length && normalizedQuery === '' ?
        selectors.length + ' records available.' :
        'Showing ' + matchingCount + ' of ' + selectors.length + ' records.';
    container.find('#datasetFilterStatus').text(status);
}

function resizeCardSelectorList() {
    $('.card-selector-scroll').each(function() {
        let top = this.getBoundingClientRect().top;
        this.style.height = Math.max(0, window.innerHeight - top - 75) + 'px';
    });
}

function renderCard(id, card) {
    return '<div class="row row-cols-1 g-4">' +
        '<div class="col">' +
        '<div class="card h-100">' +
        '<div class="card-header bg-primary text-white d-flex justify-content-between align-items-center gap-3">' +
        '<h3 class="card-title mb-0">' + escapeHtml(card.title ?? '') + '</h3>' +
        (card.learnMoreUrl ?
            '<a class="dataset-learn-more" href="' + escapeHtml(card.learnMoreUrl) + '" target="_blank" rel="noopener noreferrer">Learn More</a>' :
            '') +
        '</div>' + // card-header
        '<div class="card-body">' +
        (typeof renderCardChartContainer === 'function' ? renderCardChartContainer(id, card.chart) : '') +
        renderCardRows(id, card.rows) +
        '</div>' + // card-body
        '</div>' + // card
        '</div>' + // col
        '</div>'; // row
}

function isAccordionRow(row) {
    return row
        && !Array.isArray(row)
        && Array.isArray(row.headers)
        && Array.isArray(row.rows);
}

function renderCardRows(baseId, rows) {
    if (!rows || rows.length === 0) return '';

    let html = '<div class="row g-2 mb-3">';

    rows.forEach(function (row, rowIndex) {
        if (!row) return;

        if (isAccordionRow(row)) {
            html += '<div class="col-sm-12" id="' + baseId + '-row-' + rowIndex + '">' +
                renderDataTableAccordion(
                    baseId + '-row-' + rowIndex + '-accordion',
                    row.title ?? 'Details',
                    row.headers,
                    row.rows,
                    row.beginExpanded === true
                ) +
                '</div>';
            return;
        }

        if (!Array.isArray(row) || row.length === 0) return;

        html += '<div class="col-sm-12" id="' + baseId + '-row-' + rowIndex + '">';
        html += '<div class="row g-2">';

        row.forEach(function(cell, cellIndex) {
            html += '<div class="col" id="' + baseId + '-row-' + rowIndex + '-cell-' + cellIndex + '">';
            html += renderCardRowCell(cell);
            html += '</div>';
        });

        html += '</div>';
        html += '</div>';
    });

    html += '</div>'; // row

    return html;
}

function isLabeledCardCell(cell) {
    return cell
        && typeof cell === 'object'
        && !Array.isArray(cell)
        && Object.prototype.hasOwnProperty.call(cell, 'label')
        && Object.prototype.hasOwnProperty.call(cell, 'value');
}

function renderCardRowCell(cell) {
    if (isLabeledCardCell(cell)) {
        return '<div class="dataset-card-row-cell h-100">' +
            '<div class="dataset-card-row-label">' + escapeHtml(cell.label ?? '') + '</div>' +
            '<div class="dataset-card-row-value p-3 h-100">' + (cell.value ?? '') + '</div>' +
            '</div>';
    }

    return '<div class="dataset-card-row-value p-3 h-100">' +
        (cell ?? '') +
        '</div>';
}

function renderDataTableAccordion(baseId, title, headers, rows, beginExpanded = false) {
    if (!rows || rows.length === 0) return '';

    let buttonCollapsedClass = beginExpanded ? '' : ' collapsed';
    let collapseShowClass = beginExpanded ? ' show' : '';
    let ariaExpanded = beginExpanded ? 'true' : 'false';

    return '<div class="accordion accordion-flush border rounded dataset-table-accordion" id="' + baseId + '">' +
        '<div class="accordion-item">' +
        '<h2 class="accordion-header" id="heading-' + baseId + '">' +
        '<button class="accordion-button' + buttonCollapsedClass + ' py-2" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-' + baseId + '" aria-expanded="' + ariaExpanded + '" aria-controls="collapse-' + baseId + '">' +
        escapeHtml(title) + ' (' + rows.length + ')' +
        '</button>' +
        '</h2>' + // accordion-header
        '<div id="collapse-' + baseId + '" class="accordion-collapse collapse' + collapseShowClass + '" aria-labelledby="heading-' + baseId + '" data-bs-parent="#' + baseId + '">' +
        '<div class="accordion-body">' +
        '<div class="table-responsive">' +
        '<table class="table table-sm table-striped table-hover align-middle mb-0">' +
        '<thead>' +
        '<tr>' +
        renderDataTableHeaders(baseId, headers) +
        '</tr>' +
        '</thead>' +
        '<tbody>' +
        renderDataTableRows(baseId, rows) +
        '</tbody>' +
        '</table>' +
        '</div>' + // table-responsive
        '</div>' + // accordion-body
        '</div>' + // accordion-collapse
        '</div>' + // accordion-item
        '</div>'; // accordion
}

function renderDataTableHeaders(baseId, headers) {
    if (!headers || headers.length === 0) return '';

    let html = '';

    headers.forEach(function(header, index) {
        html += '<th scope="col" class="text-nowrap" id="' + baseId + '-header-' + index + '">';
        html += header ?? '';
        html += '</th>';
    });

    return html;
}

function renderDataTableRows(baseId, rows) {
    if (!rows || rows.length === 0) return '';

    let html = '';

    rows.forEach(function(row, rowIndex) {
        if (!row || row.length === 0) return;

        html += '<tr id="' + baseId + '-row-' + rowIndex + '">';

        row.forEach(function(cell, cellIndex) {
            html += '<td id="' + baseId + '-row-' + rowIndex + '-cell-' + cellIndex + '">';
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
            let progressDetailsExpanded = $('#progressDetails').hasClass('show');

            $(el).html(renderProgressData(progressData, progressDetailsExpanded));
            $(el).removeClass('hidden');

            if (isAnyProgressRunning(progressData)) {
                setTimeout(refreshProgress, 5000);

            } else if (isAllProgressComplete(progressData)) {
                logCompletedProgressErrors(progressData);

                let modelsContainer = $('#modelsContainer');
                let hasRenderedModels = modelsContainer.find('.card-panel, .card-selector, .card, .home-patient-card').length > 0;
                let isShowingNoModelsMessage = modelsContainer.find('.alert[role="status"]').length > 0;

                if (!hasRenderedModels && !isShowingNoModelsMessage) {
                    modelsContainer.html(renderNoModelsCompletedMessage());
                }

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
        if (_callback) {
            _callback(progressData);
        }
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

function logCompletedProgressErrors(progressData) {
    if (!Array.isArray(progressData) || progressData.length === 0) {
        return;
    }

    let progressWithErrors = progressData.filter(item => item.errors && item.errors.length > 0);
    if (progressWithErrors.length === 0) {
        return;
    }

    console.group('Progress completed with errors');

    progressWithErrors.forEach(function(item) {
        console.group(item.label ?? 'Progress Item');

        if (item.message) {
            console.log(item.message);
        }

        item.errors.forEach(function(error) {
            console.error(error);
        });

        console.groupEnd();
    });

    console.groupEnd();
}

function clearProgress() {
    let el = $('#progressContainer');
    $(el).html('');
    $(el).addClass('hidden');
}

function getProgressItemPercentComplete(item) {
    if (!item) return 0;

    if (item.status === 'COMPLETED') {
        return 100;
    } else if (item.status === 'WAITING_TO_START') {
        return 0;
    } else if (item.status === 'RUNNING' && item.percentComplete !== undefined && item.percentComplete !== null) {
        return Math.max(0, Math.min(100, Number(item.percentComplete)));
    }

    return 0;
}

function getProgressSummaryPercentComplete(progressData) {
    if (!Array.isArray(progressData) || progressData.length === 0) {
        return 0;
    }

    let totalPercentComplete = 0;
    progressData.forEach(function(item) {
        totalPercentComplete += getProgressItemPercentComplete(item);
    });

    return Math.round(totalPercentComplete / progressData.length);
}

function escapeHtml(value) {
    return String(value ?? '')
        .replaceAll('&', '&amp;')
        .replaceAll('<', '&lt;')
        .replaceAll('>', '&gt;')
        .replaceAll('"', '&quot;')
        .replaceAll("'", '&#039;');
}

function groupProgressByEndpoint(progressData) {
    let endpointGroups = new Map();

    if (!Array.isArray(progressData)) {
        return endpointGroups;
    }

    progressData.forEach(function(item) {
        let endpointName = item.endpointName || 'Unknown Endpoint';

        if (!endpointGroups.has(endpointName)) {
            endpointGroups.set(endpointName, []);
        }

        endpointGroups.get(endpointName).push(item);
    });

    return endpointGroups;
}

function buildSafeProgressId(value) {
    return String(value ?? 'progress')
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '') || 'progress';
}

function getProgressGroupStatus(progressItems) {
    if (!Array.isArray(progressItems) || progressItems.length === 0) {
        return 'WAITING_TO_START';
    }

    if (progressItems.every(item => item.status === 'COMPLETED')) {
        return 'COMPLETED';
    }

    if (progressItems.some(item => item.status === 'RUNNING')) {
        return 'RUNNING';
    }

    return 'WAITING_TO_START';
}

function renderProgressBar(percentComplete, label, extraCssClass) {
    let safePercentComplete = Math.max(0, Math.min(100, Number(percentComplete) || 0));

    return '<div class="progress progress-display ' + (extraCssClass ?? '') + '">' +
        '<div class="progress-bar" role="progressbar" style="width: ' + safePercentComplete + '%;"' +
        ' aria-valuenow="' + safePercentComplete + '" aria-valuemin="0" aria-valuemax="100"></div>' +
        '<div class="progress-label">' + escapeHtml(label) + '</div>' +
        '</div>';
}

function renderProgressItem(item) {
    let itemPercentComplete = getProgressItemPercentComplete(item);
    let itemLabel = (item.label ?? 'Progress Item') + ': ' + itemPercentComplete + '% Complete';

    if (item.message) {
        itemLabel += ' - ' + item.message;
    }

    let html = '<div class="progress-detail-item">' +
        renderProgressBar(itemPercentComplete, itemLabel, 'progress-detail-bar');

    if (item.errors && item.errors.length > 0) {
        html += '<div class="progress-errors">' +
            '<div class="progress-errors-label">Errors:</div>' +
            '<ul class="mb-0">';

        $.each(item.errors, function(j, error) {
            html += '<li>' + escapeHtml(error) + '</li>';
        });

        html += '</ul>' +
            '</div>';
    }

    html += '</div>';

    return html;
}

function renderEndpointProgressGroup(endpointName, progressItems, endpointExpanded = false) {
    let endpointId = 'progress-endpoint-' + buildSafeProgressId(endpointName);
    let endpointPercentComplete = getProgressSummaryPercentComplete(progressItems);
    let endpointHasErrors = progressItems.some(item => item.errors && item.errors.length > 0);
    let endpointLabel = endpointName + ': ' + endpointPercentComplete + '% Complete' +
        (endpointHasErrors ? ' - Some items have errors' : '');

    let collapseClass = endpointExpanded ? ' show' : '';
    let buttonCollapsedClass = endpointExpanded ? '' : ' collapsed';
    let ariaExpanded = endpointExpanded ? 'true' : 'false';

    let html = '<div class="accordion endpoint-progress-accordion" id="' + endpointId + '-accordion">' +
        '<div class="accordion-item endpoint-progress-item">' +
        '<h3 class="accordion-header" id="' + endpointId + '-heading">' +
        '<button class="accordion-button endpoint-progress-button' + buttonCollapsedClass + '" type="button"' +
        ' data-bs-toggle="collapse" data-bs-target="#' + endpointId + '-details"' +
        ' aria-expanded="' + ariaExpanded + '" aria-controls="' + endpointId + '-details">' +
        renderProgressBar(endpointPercentComplete, endpointLabel, 'endpoint-progress-bar') +
        '</button>' +
        '</h3>' +
        '<div id="' + endpointId + '-details" class="accordion-collapse collapse' + collapseClass + '"' +
        ' aria-labelledby="' + endpointId + '-heading">' +
        '<div class="accordion-body endpoint-progress-details">';

    progressItems.forEach(function(item) {
        html += renderProgressItem(item);
    });

    html += '</div>' +
        '</div>' +
        '</div>' +
        '</div>';

    return html;
}

function renderProgressData(progressData, detailsExpanded = false) {
    if (!Array.isArray(progressData) || progressData.length === 0) {
        return '';
    }

    let endpointGroups = groupProgressByEndpoint(progressData);
    let summaryPercentComplete = getProgressSummaryPercentComplete(progressData);
    let hasErrors = progressData.some(item => item.errors && item.errors.length > 0);
    let summaryLabel = 'Overall Progress: ' + summaryPercentComplete + '% Complete' +
        (hasErrors ? ' - Some items have errors' : '');
    let collapseClass = detailsExpanded ? ' show' : '';
    let buttonCollapsedClass = detailsExpanded ? '' : ' collapsed';
    let ariaExpanded = detailsExpanded ? 'true' : 'false';

    let expandedEndpointIds = new Set();
    $('#progressDetails .endpoint-progress-accordion .accordion-collapse.show').each(function() {
        expandedEndpointIds.add(this.id);
    });

    let html = '<div class="accordion progress-summary-accordion" id="progressAccordion">' +
        '<div class="accordion-item progress-summary-item">' +
        '<h2 class="accordion-header" id="progressSummaryHeading">' +
        '<button class="accordion-button progress-summary-button' + buttonCollapsedClass + '" type="button"' +
        ' data-bs-toggle="collapse" data-bs-target="#progressDetails"' +
        ' aria-expanded="' + ariaExpanded + '" aria-controls="progressDetails">' +
        renderProgressBar(summaryPercentComplete, summaryLabel, 'progress-summary-bar') +
        '</button>' +
        '</h2>' +
        '<div id="progressDetails" class="accordion-collapse collapse' + collapseClass + '"' +
        ' aria-labelledby="progressSummaryHeading" data-bs-parent="#progressAccordion">' +
        '<div class="accordion-body progress-details">';

    endpointGroups.forEach(function(endpointItems, endpointName) {
        let endpointId = 'progress-endpoint-' + buildSafeProgressId(endpointName) + '-details';
        html += renderEndpointProgressGroup(
            endpointName,
            endpointItems,
            expandedEndpointIds.has(endpointId)
        );
    });

    html += '</div>' +
        '</div>' +
        '</div>' +
        '</div>';

    return html;
}

function getDataSets() {
    return $('#dataSets').html().split(',').map(s => s.trim());
}

let eventSource = null;

function initializeSSE(_callback) {
    if (eventSource !== null) {
        eventSource.close();
    }

    eventSource = new EventSource(getBasePath() + '/sse');

    eventSource.addEventListener("dataset-update", function(event) {
        const eventData = JSON.parse(event.data);
        if (getDataSets().includes(eventData.dataSet)) {
            console.log("dataset-update: dataSet=" + eventData.dataSet + ", endpoint=" + eventData.endpoint);
            getUpdatedModels(_callback);
        }
    });

    eventSource.addEventListener("endpoint-population-started", function(event) {
        const eventData = JSON.parse(event.data);
        console.log("endpoint-population-started: endpoint=" + eventData.endpoint);
    });

    eventSource.addEventListener("endpoint-population-complete", function(event) {
        const eventData = JSON.parse(event.data);
        console.log("endpoint-population-complete: endpoint=" + eventData.endpoint);
    });

    eventSource.addEventListener("all-complete", function(event) {
        console.log("all-complete");
        $('#refresh').removeAttr('aria-disabled').removeClass('disabled').show();
    });
}

window.addEventListener("pagehide", function() {
    if (eventSource !== null) {
        eventSource.close();
        eventSource = null;
    }
});

function getUpdatedModels(_callback) {
    $.ajax({
        method: "POST",
        url: getBasePath() + '/models'
    }).done(function(models) {
        _callback(models);
    });
}

function renderNoModelsCompletedMessage() {
    return '<div class="alert alert-info" role="status">' +
        'No records were found for this section after the data load completed.' +
        '</div>';
}

function renderModels(models) {
    let currentFilter = $('#datasetFilter').val();
    if (currentFilter !== undefined) {
        datasetFilterQuery = currentFilter;
    }

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
            applyDatasetFilter();
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

    } else {
        getCurrentProgress(function(progressData) {
            if (isAllProgressComplete(progressData)) {
                $('#modelsContainer').html(renderNoModelsCompletedMessage());
            }
        });
    }

    if (typeof renderCharts === 'function') {
        renderCharts();
    }
}

$(document).on('input', '#datasetFilter', function() {
    datasetFilterQuery = $(this).val();
    applyDatasetFilter();
});

$(document).on('click', '#modelsContainer .card-selector-sort-button', function() {
    let button = $(this);
    let sortIndex = button.attr('data-sort-index');
    let layout = button.closest('.card-selector-layout');
    let list = layout.find('.card-selector-items');
    let currentDirection = button.attr('data-sort-direction');
    let nextDirection = currentDirection === 'asc' ? 'desc' : 'asc';

    layout.find('.card-selector-sort-button')
        .removeAttr('data-sort-direction')
        .attr('aria-sort', 'none')
        .find('.card-selector-sort-indicator')
        .text('');

    button.attr('data-sort-direction', nextDirection)
        .attr('aria-sort', nextDirection === 'asc' ? 'ascending' : 'descending')
        .find('.card-selector-sort-indicator')
        .text(nextDirection === 'asc' ? ' ▲' : ' ▼');

    let sortedSelectors = list.children('.card-selector').get().sort(function(a, b) {
        let aValue = $(a).attr('data-sort-value-' + sortIndex) ?? '';
        let bValue = $(b).attr('data-sort-value-' + sortIndex) ?? '';
        let comparison = aValue.localeCompare(bValue, undefined, {
            numeric: true,
            sensitivity: 'base'
        });

        return nextDirection === 'asc' ? comparison : -comparison;
    });

    list.append(sortedSelectors);
});

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