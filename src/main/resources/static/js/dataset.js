function getCardSelectorSortValue(value) {
    return $('<div>').html(value === undefined || value === null ? '' : value).text().trim().toLowerCase();
}

function reportAndRenderModelsError(error, context) {
    if (typeof window.handleFrontEndException === 'function') {
        window.handleFrontEndException(error, context);
    } else {
        logError('Front-end rendering error: ' + error);
    }

    let container = $('#modelsContainer');
    if (container.length > 0) {
        container.html(
            '<div class="alert alert-danger" role="alert">' +
            'Unable to display records for this section. Please refresh the page or try again later.' +
            '</div>'
        );
    }
}

let serverRequestsEnabled = true;
let datasetPageUnloading = false;
let progressRefreshTimer = null;
let eventSource = null;
let eventSourceFallbackInterval = null;

function isServerUnavailableResponse(jqXHR) {
    return !jqXHR || jqXHR.status === 0;
}

function isRequestAborted(jqXHR, textStatus) {
    return textStatus === 'abort' || (jqXHR && jqXHR.readyState === 0 && jqXHR.status === 0);
}

function isUnauthorizedResponse(jqXHR) {
    return jqXHR && (jqXHR.status === 401 || jqXHR.status === 403);
}

function handleDatasetUnauthorized() {
    stopDatasetServerActivity();

    if (typeof redirectToUnauthorized === 'function') {
        redirectToUnauthorized();
        return;
    }

    window.location.href = '/unauthorized';
}

function getAjaxFailureMessage(jqXHR, textStatus, errorThrown, fallbackMessage) {
    if (jqXHR && jqXHR.responseText) {
        return jqXHR.responseText;
    }

    if (errorThrown) {
        return String(errorThrown);
    }

    if (textStatus) {
        return fallbackMessage + ' (' + textStatus + ')';
    }

    return fallbackMessage;
}

function stopDatasetServerActivity() {
    serverRequestsEnabled = false;

    if (progressRefreshTimer !== null) {
        clearTimeout(progressRefreshTimer);
        progressRefreshTimer = null;
    }

    if (eventSourceFallbackInterval !== null) {
        clearInterval(eventSourceFallbackInterval);
        eventSourceFallbackInterval = null;
    }

    if (eventSource !== null) {
        eventSource.close();
        eventSource = null;
    }
}

function resumeDatasetServerActivity() {
    serverRequestsEnabled = true;
    datasetPageUnloading = false;
}

function handleDatasetServerUnavailable(context) {
    stopDatasetServerActivity();
    logWarn('Stopping dataset server requests because the server is unavailable: ' + context);
}

function shouldContinueDatasetServerRequests() {
    return serverRequestsEnabled && !datasetPageUnloading;
}

let hiddenSourceEndpointNames = new Set();
let sourceGroupedModels = new Map();

function getAllDataBySourceEndpointName(model) {
    let groups = model && model.allDataBySourceEndpointName;
    return groups && typeof groups === 'object' && !Array.isArray(groups) ? groups : {};
}

function getVisibleSourceRecords(model, dateProperty) {
    let groups = getAllDataBySourceEndpointName(model);
    let records = [];

    Object.keys(groups).forEach(function(source) {
        let sourceName = getSourceEndpointName(source);
        if (hiddenSourceEndpointNames.has(sourceName) || !Array.isArray(groups[source])) {
            return;
        }

        groups[source].forEach(function(record) {
            if (!record || typeof record !== 'object' || Array.isArray(record)) {
                return;
            }

            // Copy records so filtering never changes the server data used to restore sources.
            let copy = Object.assign({}, record, { sourceEndpointName: sourceName });
            let date = copy[dateProperty];
            // Normalize Java offsets to ISO 8601 for consistent parsing across browsers.
            if (typeof date === 'string') {
                date = date.replace(/([+-]\d{2})(\d{2})$/, '$1:$2');
                copy[dateProperty] = date;
            }
            let timestamp = typeof date === 'number' || (typeof date === 'string' && date.trim() !== '') ?
                new Date(date).getTime() : NaN;
            records.push({
                record: copy,
                timestamp: Number.isFinite(timestamp) ? timestamp : -Infinity,
                index: records.length
            });
        });
    });

    return records.sort(function(a, b) {
        // Keep undated records last and explicitly preserve ties, independent of sort stability.
        return a.timestamp === b.timestamp ? a.index - b.index : (a.timestamp > b.timestamp ? -1 : 1);
    }).map(function(entry) {
        return entry.record;
    });
}

function hasVisibleSourceEndpoint(source) {
    return getSourceEndpointNames(source)
        .some(function(sourceEndpointName) {
            return !hiddenSourceEndpointNames.has(sourceEndpointName);
        });
}

function getSourceEndpointNamesFromModels(models) {
    let sourceNames = [];

    if (!Array.isArray(models)) {
        return sourceNames;
    }

    models.forEach(function(model) {
        let sources = [];

        if (model && model.card && model.card.source !== undefined) {
            sources = getSourceEndpointNames(model.card.source);
        } else if (model && model.source !== undefined) {
            sources = getSourceEndpointNames(model.source);
        } else {
            sources = getSourceEndpointNames(model ? model.sourceEndpointName : undefined);
        }

        sources.forEach(function(sourceName) {
            if (sourceNames.indexOf(sourceName) === -1) {
                sourceNames.push(sourceName);
            }
        });
    });

    return sourceNames;
}

function renderSourceEndpointLegend(sourceEndpointNames, interactive) {
    interactive = interactive !== false;

    if (!Array.isArray(sourceEndpointNames) || sourceEndpointNames.length === 0) {
        return '';
    }

    let html = '<div class="source-endpoint-legend" aria-label="' + (interactive ? 'Filter by source' : 'Record sources') + '">' +
        '<div class="source-endpoint-legend-label">Sources</div>' +
        '<div class="source-endpoint-legend-items">';

    sourceEndpointNames.forEach(function(sourceEndpointName) {
        let normalizedSourceName = getSourceEndpointName(sourceEndpointName);
        let isHidden = interactive && hiddenSourceEndpointNames.has(normalizedSourceName);

        if (interactive) {
            html += '<button type="button" class="source-endpoint-legend-item source-endpoint-tinted' +
                (isHidden ? ' source-endpoint-hidden' : '') + '"' +
                ' data-source-endpoint-name="' + escapeHtml(normalizedSourceName) + '"' +
                ' data-source-endpoint-interactive="true"' +
                ' aria-pressed="' + (isHidden ? 'true' : 'false') + '"' +
                ' title="' + (isHidden ? 'Show ' : 'Hide ') + escapeHtml(normalizedSourceName) + '"' +
                renderSourceEndpointStyle(normalizedSourceName) + '>' +
                '<span class="source-endpoint-legend-swatch" aria-hidden="true"></span>' +
                '<span class="source-endpoint-legend-text">' + escapeHtml(normalizedSourceName) + '</span>' +
                '</button>';
        } else {
            html += '<span class="source-endpoint-legend-item source-endpoint-tinted source-endpoint-legend-item-static"' +
                ' data-source-endpoint-name="' + escapeHtml(normalizedSourceName) + '"' +
                ' data-source-endpoint-interactive="false"' +
                ' title="' + escapeHtml(normalizedSourceName) + '"' +
                renderSourceEndpointStyle(normalizedSourceName) + '>' +
                '<span class="source-endpoint-legend-swatch" aria-hidden="true"></span>' +
                '<span class="source-endpoint-legend-text">' + escapeHtml(normalizedSourceName) + '</span>' +
                '</span>';
        }
    });

    html += '</div>' +
        '</div>';

    return html;
}

function renderCardSelectors(id, cardSelector, card, selected) {
    let sortAttributes = '';
    let primarySourceEndpointName = getPrimarySourceEndpointName(card ? card.source : undefined);
    let sourceEndpointNames = getSourceEndpointDataAttribute(card ? card.source : undefined);

    if (Array.isArray(cardSelector)) {
        cardSelector.forEach(function(selectorItem, index) {
            let valueForSort = selectorItem.valueForCompare !== undefined && selectorItem.valueForCompare !== null ?
                selectorItem.valueForCompare :
                selectorItem.value;

            sortAttributes += ' data-sort-value-' + index + '="' + escapeHtml(getCardSelectorSortValue(valueForSort)) + '"';
        });
    }

    let html = '<button type="button" class="list-group-item list-group-item-action card-selector source-endpoint-tinted' +
        (selected ? ' active' : '') + '"' +
        ' id="' + id + '-selector" data-card-target="' + id + '-panel"' +
        ' aria-controls="' + id + '-panel" aria-selected="' + (selected ? 'true' : 'false') + '"' +
        ' data-source-endpoint-name="' + escapeHtml(sourceEndpointNames) + '"' +
        renderSourceEndpointStyle(primarySourceEndpointName) +
        sortAttributes + '>';

    if (Array.isArray(cardSelector)) {
        html += '<div class="row g-4 row-cols-sm-' + cardSelector.length + ' g-4 card-selector-fields">';
        cardSelector.forEach(function(selectorItem) {
            html += '<div class="col-sm-' + selectorItem.bootstrapWidth + ' card-selector-field">' +
                safeTextValue(selectorItem.value, '') +
                '</div>';
        });
        html += '</div>';
    } else {
        html += safeTextValue(cardSelector, '');
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
            '<span class="card-selector-sort-text">' + escapeHtml(safeTextValue(selectorItem.label, '')) + '</span>' +
            '</span>' +
            '<span class="card-selector-sort-indicator" aria-hidden="true"></span>' +
            '</button>' +
            '</div>';
    });

    html += '</div>' +
        '</div>';

    return html;
}

function renderDatasetFilter(query) {
    query = safeTextValue(query, '');

    return '<div class="dataset-filter">' +
        '<label class="form-label" for="datasetFilter">Filter records</label>' +
        '<input class="form-control" id="datasetFilter" type="search"' +
        ' value="' + escapeHtml(query) + '"' +
        ' placeholder="Type to filter these records" autocomplete="off"' +
        ' aria-describedby="datasetFilterStatus">' +
        '<div class="dataset-filter-status" id="datasetFilterStatus" role="status" aria-live="polite"></div>' +
        '</div>';
}

function renderCardSelectorLayout(items, sourceEndpointNames) {
    items = Array.isArray(items) ? items : [];
    sourceEndpointNames = Array.isArray(sourceEndpointNames) ? sourceEndpointNames : [];

    let selectors = [];
    let cards = [];

    items.forEach(function(item, index) {
        item = item || {};

        let id = 'card_' + (index + 1);
        selectors.push(renderCardSelectors(id, item.selector, item.card, index === 0));
        cards.push('<div class="card-panel' + (index === 0 ? '' : ' d-none') + '"' +
            ' id="' + id + '-panel" role="tabpanel" aria-labelledby="' + id + '-selector"' +
            (index === 0 ? '' : ' aria-hidden="true"') + '>' +
            renderCard(id, item.card) +
            '</div>');
    });

    return renderSourceEndpointLegend(sourceEndpointNames) +
        renderDatasetFilter(datasetFilterQuery) +
        '<div class="row g-4 card-selector-layout">' +
        '<div class="col-6 d-none d-md-block">' +
        '<div class="card-selector-list-container">' +
        renderCardSelectorHeaders(items.length > 0 ? items[0].selector : null) +
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

    let normalizedQuery = query.toLowerCase();

    $(element).contents().each(function() {
        if (this.nodeType === Node.TEXT_NODE) {
            let text = this.nodeValue;
            let normalizedText = text.toLowerCase();
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
    try {
        let container = $('#modelsContainer');
        let layout = container.find('.card-selector-layout');
        let normalizedQuery = datasetFilterQuery.trim().toLowerCase();
        let query = datasetFilterQuery.trim();
        let selectors = layout.find('.card-selector');
        let matchingCount = 0;

        clearDatasetFilterHighlights(container);

        selectors.each(function() {
            let selector = $(this);
            let panel = layout.find('#' + selector.attr('data-card-target'));
            let sourceEndpointNames = safeTextValue(selector.attr('data-source-endpoint-name'), '');
            let sourceIsVisible = sourceEndpointNames !== '' && hasVisibleSourceEndpoint(sourceEndpointNames.split('|'));
            let textMatches = normalizedQuery === '' || panel.text().toLowerCase().indexOf(normalizedQuery) !== -1;
            let matches = sourceIsVisible && textMatches;

            selector.toggleClass('dataset-filtered-out', !matches);
            panel.toggleClass('dataset-filtered-out', !matches);
            applyDataTableSourceVisibility(panel);

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

        let status = matchingCount === selectors.length && normalizedQuery === '' && hiddenSourceEndpointNames.size === 0 ?
            selectors.length + ' records available.' :
            'Showing ' + matchingCount + ' of ' + selectors.length + ' records.';
        container.find('#datasetFilterStatus').text(status);

        if (typeof renderCharts === 'function') {
            renderCharts();
        }

        scheduleCardSelectorListResize();

    } catch (error) {
        reportAndRenderModelsError(error, 'applyDatasetFilter');
    }
}

function applySourceEndpointVisibility(container) {
    container = container || $('#modelsContainer');

    try {
        container.find('.source-endpoint-legend-item[data-source-endpoint-interactive="true"]').each(function() {
            let button = $(this);
            let sourceEndpointName = getSourceEndpointName(button.attr('data-source-endpoint-name'));
            let isHidden = hiddenSourceEndpointNames.has(sourceEndpointName);

            button.toggleClass('source-endpoint-hidden', isHidden);
            button.attr('aria-pressed', isHidden ? 'true' : 'false');
            button.attr('title', (isHidden ? 'Show ' : 'Hide ') + sourceEndpointName);
        });

        let layout = container.find('.card-selector-layout');
        if (layout.length > 0) {
            applyDatasetFilter();
            return;
        }

        container.find('.home-summary-card, .home-patient-card').each(function() {
            let card = $(this);
            let sourceEndpointNames = safeTextValue(card.attr('data-source-endpoint-name'), '');
            card.toggleClass('dataset-filtered-out', !hasVisibleSourceEndpoint(sourceEndpointNames.split('|')));
        });

        if (typeof renderCharts === 'function') {
            renderCharts();
        }

    } catch (error) {
        reportAndRenderModelsError(error, 'applySourceEndpointVisibility');
    }
}

function resizeCardSelectorList() {
    const minimumListHeight = 240;

    $('.card-selector-layout').each(function() {
        let layout = $(this);
        let listContainer = layout.find('.card-selector-list-container').first();
        let scroll = layout.find('.card-selector-scroll').first();
        let header = layout.find('.card-selector-header').first();
        let items = layout.find('.card-selector-items').first();
        let activePanel = layout.find('.card-panel:not(.d-none):not(.dataset-filtered-out)').first();

        if (listContainer.length === 0 || scroll.length === 0 || items.length === 0) {
            return;
        }

        let previousScrollTop = scroll[0].scrollTop;

        listContainer.css('height', '');
        listContainer.css('min-height', '');
        scroll.css('height', '');
        scroll.css('min-height', '');

        if (typeof window.matchMedia === 'function' && window.matchMedia('(max-width: 767.98px)').matches) {
            scroll[0].scrollTop = previousScrollTop;
            return;
        }

        let containerRect = listContainer[0].getBoundingClientRect();
        let containerTop = containerRect.top;
        let availableViewportHeight = Math.max(minimumListHeight, window.innerHeight - containerTop - 75);
        let activePanelAlignedHeight = minimumListHeight;

        if (activePanel.length > 0) {
            let activePanelRect = activePanel[0].getBoundingClientRect();
            activePanelAlignedHeight = Math.max(minimumListHeight, activePanelRect.bottom - containerTop);
        }

        let baselineHeight = activePanelAlignedHeight;

        let headerHeight = header.length > 0 ? header[0].getBoundingClientRect().height : 0;
        let selectorItemsHeight = items[0].scrollHeight;
        let contentHeight = headerHeight + selectorItemsHeight;

        let targetContainerHeight;
        if (contentHeight <= baselineHeight) {
            targetContainerHeight = baselineHeight;
        } else {
            targetContainerHeight = Math.min(contentHeight, Math.max(baselineHeight, availableViewportHeight));
        }

        listContainer.css('height', targetContainerHeight + 'px');
        listContainer.css('min-height', baselineHeight + 'px');

        let targetScrollHeight = Math.max(0, listContainer[0].clientHeight - headerHeight);
        scroll.css('height', targetScrollHeight + 'px');
        scroll.css('min-height', '0');

        let maxScrollTop = Math.max(0, scroll[0].scrollHeight - scroll[0].clientHeight);
        scroll[0].scrollTop = Math.min(previousScrollTop, maxScrollTop);
    });
}

function scheduleCardSelectorListResize() {
    let nextFrame = typeof window.requestAnimationFrame === 'function' ?
        window.requestAnimationFrame :
        function(callback) {
            window.setTimeout(callback, 0);
        };

    nextFrame(function() {
        resizeCardSelectorList();

        nextFrame(function() {
            resizeCardSelectorList();
        });
    });

    setTimeout(resizeCardSelectorList, 150);
}

function renderCard(id, card) {
    card = card || {};

    let primarySourceEndpointName = getPrimarySourceEndpointName(card.source);
    let sourceEndpointNames = getSourceEndpointDataAttribute(card.source);

    return '<div class="row row-cols-1 g-4">' +
        '<div class="col">' +
        '<div class="card h-100 source-endpoint-tinted" data-source-endpoint-name="' + escapeHtml(sourceEndpointNames) + '"' +
        renderSourceEndpointStyle(primarySourceEndpointName) + '>' +
        '<div class="card-header bg-primary text-white d-flex justify-content-between align-items-center gap-3">' +
        '<h3 class="card-title mb-0">' + escapeHtml(safeTextValue(card.title, '')) + '</h3>' +
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
    if (!Array.isArray(rows) || rows.length === 0) return '';

    let html = '<div class="row g-2 mb-3">';

    rows.forEach(function (row, rowIndex) {
        if (!row) return;

        if (isAccordionRow(row)) {
            html += '<div class="col-sm-12" id="' + baseId + '-row-' + rowIndex + '">' +
                renderDataTableAccordion(
                    baseId + '-row-' + rowIndex + '-accordion',
                    safeTextValue(row.title, 'Details'),
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
            '<div class="dataset-card-row-label">' + escapeHtml(safeTextValue(cell.label, '')) + '</div>' +
            '<div class="dataset-card-row-value p-3 h-100">' + safeTextValue(cell.value, '') + '</div>' +
            '</div>';
    }

    return '<div class="dataset-card-row-value p-3 h-100">' +
        safeTextValue(cell, '') +
        '</div>';
}

function renderDataTableAccordion(baseId, title, headers, rows, beginExpanded) {
    beginExpanded = beginExpanded === true;

    if (!Array.isArray(rows) || rows.length === 0) return '';

    let buttonCollapsedClass = beginExpanded ? '' : ' collapsed';
    let collapseShowClass = beginExpanded ? ' show' : '';
    let ariaExpanded = beginExpanded ? 'true' : 'false';

    return '<div class="accordion accordion-flush border rounded dataset-table-accordion" id="' + baseId + '">' +
        '<div class="accordion-item">' +
        '<h2 class="accordion-header" id="heading-' + baseId + '">' +
        '<button class="accordion-button' + buttonCollapsedClass + ' py-2" type="button" data-bs-toggle="collapse" data-bs-target="#collapse-' + baseId + '" aria-expanded="' + ariaExpanded + '" aria-controls="collapse-' + baseId + '">' +
        '<span class="dataset-table-accordion-title">' + escapeHtml(title) + '</span>' +
        ' <span class="dataset-table-accordion-count" aria-label="' + rows.length + ' visible rows">(' + rows.length + ')</span>' +
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

function updateDataTableAccordionCounts(container) {
    $(container).find('.dataset-table-accordion').each(function() {
        let accordion = $(this);
        let visibleRowCount = accordion.find('tbody tr').filter(function() {
            return !$(this).hasClass('dataset-filtered-out');
        }).length;
        let count = accordion.find('.dataset-table-accordion-count').first();

        count.text('(' + visibleRowCount + ')');
        count.attr('aria-label', visibleRowCount + ' visible rows');
    });
}

function renderDataTableHeaders(baseId, headers) {
    if (!Array.isArray(headers) || headers.length === 0) return '';

    let html = '';

    headers.forEach(function(header, index) {
        html += '<th scope="col" class="text-nowrap" id="' + baseId + '-header-' + index + '">';
        html += safeTextValue(header, '');
        html += '</th>';
    });

    return html;
}

function renderDataTableRows(baseId, rows) {
    if (!Array.isArray(rows) || rows.length === 0) return '';

    let html = '';

    rows.forEach(function(row, rowIndex) {
        let rowData = getDataTableRowData(row);

        if (!Array.isArray(rowData) || rowData.length === 0) return;

        let rowSource = getDataTableRowSource(row);
        let rowPrimarySourceEndpointName = getPrimarySourceEndpointName(rowSource);
        let rowSourceEndpointNames = getSourceEndpointDataAttribute(rowSource);
        let rowHasSource = rowSource !== undefined && rowSource !== null;
        let rowIsVisible = !rowHasSource || hasVisibleSourceEndpoint(rowSource);

        html += '<tr id="' + baseId + '-row-' + rowIndex + '"' +
            (rowHasSource ?
                ' class="source-endpoint-tinted' + (rowIsVisible ? '' : ' dataset-filtered-out') + '"' +
                ' data-source-endpoint-name="' + escapeHtml(rowSourceEndpointNames) + '"' +
                renderSourceEndpointStyle(rowPrimarySourceEndpointName) :
                '') +
            '>';

        rowData.forEach(function(cell, cellIndex) {
            html += '<td id="' + baseId + '-row-' + rowIndex + '-cell-' + cellIndex + '">';
            html += safeTextValue(cell, '');
            html += '</td>';
        });

        html += '</tr>';
    });

    return html;
}

function applyDataTableSourceVisibility(container) {
    $(container).find('tr[data-source-endpoint-name]').each(function() {
        let row = $(this);
        let sourceEndpointNames = safeTextValue(row.attr('data-source-endpoint-name'), '');
        row.toggleClass('dataset-filtered-out', !hasVisibleSourceEndpoint(sourceEndpointNames.split('|')));
    });

    updateDataTableAccordionCounts(container);
}

function getDataTableRowData(row) {
    if (row && typeof row === 'object' && !Array.isArray(row)) {
        if (Array.isArray(row.data)) {
            return row.data;
        }

        if (Array.isArray(row.values)) {
            return row.values;
        }
    }

    return row;
}

function getDataTableRowSource(row) {
    return row && typeof row === 'object' && !Array.isArray(row) ?
        row.source :
        null;
}

function refreshProgress() {
    if (!shouldContinueDatasetServerRequests()) {
        return;
    }

    getCurrentProgress(function(progressData) {
        if (!shouldContinueDatasetServerRequests()) {
            return;
        }

        if (progressData) {
            let el = $('#progressContainer');
            let progressDetailsExpanded = $('#progressDetails').hasClass('show');

            $(el).html(renderProgressData(progressData, progressDetailsExpanded));
            $(el).removeClass('hidden');

            if (isAnyProgressRunning(progressData)) {
                progressRefreshTimer = setTimeout(refreshProgress, 5000);

            } else if (isAllProgressComplete(progressData)) {
                logCompletedProgressErrors(progressData);

                let modelsContainer = $('#modelsContainer');
                let hasRenderedModels = modelsContainer.find('.card-panel, .card-selector, .card, .home-summary-card').length > 0;
                let isShowingNoModelsMessage = modelsContainer.find('.alert[role="status"]').length > 0;

                if (!hasRenderedModels && !isShowingNoModelsMessage) {
                    modelsContainer.html(renderNoModelsCompletedMessage());
                }

                progressRefreshTimer = setTimeout(clearProgress, 30000);
            }
        }
    });
}

function getCurrentProgress(_callback) {
    if (!shouldContinueDatasetServerRequests()) {
        return;
    }

    $.ajax({
        method: "POST",
        url: getBasePath() + '/progress'
    }).done(function(progressData) {
        if (!_callback || !shouldContinueDatasetServerRequests()) {
            return;
        }

        try {
            _callback(progressData);
        } catch (error) {
            reportAndRenderModelsError(error, getBasePath() + '/progress callback');
        }
    }).fail(function(jqXHR, textStatus) {
        if (isRequestAborted(jqXHR, textStatus)) {
            return;
        }

        if (isServerUnavailableResponse(jqXHR)) {
            handleDatasetServerUnavailable(getBasePath() + '/progress');
            return;
        }

        if (isUnauthorizedResponse(jqXHR)) {
            handleDatasetUnauthorized();
        }
    });
}

function isAnyProgressRunning(progressData) {
    const runningStatuses = new Set(['WAITING_TO_START', 'RUNNING']);
    return Array.isArray(progressData)
        && progressData.some(function(item) {
            return item && runningStatuses.has(item.status);
        });
}

function isAllProgressComplete(progressData) {
    return Array.isArray(progressData)
        && progressData.length > 0
        && progressData.every(function(item) {
            return item && item.status === 'COMPLETED';
        });
}

function logCompletedProgressErrors(progressData) {
    if (!Array.isArray(progressData) || progressData.length === 0) {
        return;
    }

    let progressWithErrors = progressData.filter(function(item) {
        return item.errors && item.errors.length > 0;
    });
    if (progressWithErrors.length === 0) {
        return;
    }

    if (window.console && typeof console.group === 'function') {
        console.group('Progress completed with errors');
    } else if (window.console && typeof console.log === 'function') {
        console.log('Progress completed with errors');
    }

    progressWithErrors.forEach(function(item) {
        if (window.console && typeof console.group === 'function') {
            console.group(safeTextValue(item.label, 'Progress Item'));
        } else if (window.console && typeof console.log === 'function') {
            console.log(safeTextValue(item.label, 'Progress Item'));
        }

        if (item.message && window.console && typeof console.log === 'function') {
            console.log(item.message);
        }

        item.errors.forEach(function(error) {
            if (window.console && typeof console.error === 'function') {
                console.error(error);
            }
        });

        if (window.console && typeof console.groupEnd === 'function') {
            console.groupEnd();
        }
    });

    if (window.console && typeof console.groupEnd === 'function') {
        console.groupEnd();
    }
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
    return String(safeTextValue(value, 'progress'))
        .toLowerCase()
        .replace(/[^a-z0-9]+/g, '-')
        .replace(/^-+|-+$/g, '') || 'progress';
}

function renderProgressBar(percentComplete, label, extraCssClass) {
    let safePercentComplete = Math.max(0, Math.min(100, Number(percentComplete) || 0));

    return '<div class="progress progress-display ' + safeTextValue(extraCssClass, '') + '">' +
        '<div class="progress-bar" role="progressbar" style="width: ' + safePercentComplete + '%;"' +
        ' aria-valuenow="' + safePercentComplete + '" aria-valuemin="0" aria-valuemax="100"></div>' +
        '<div class="progress-label">' + escapeHtml(label) + '</div>' +
        '</div>';
}

let progressErrorAccordionExpandedState = {};

function buildProgressErrorAccordionKey(parentId, item, itemIndex) {
    return parentId + '-' + itemIndex + '-' + buildSafeProgressId(item.endpointName) + '-' + buildSafeProgressId(item.label);
}

function isProgressErrorAccordionExpanded(stateKey) {
    return !Object.prototype.hasOwnProperty.call(progressErrorAccordionExpandedState, stateKey) ||
        progressErrorAccordionExpandedState[stateKey] === true;
}

function renderProgressErrorsAccordion(item, parentId, itemIndex) {
    if (!item.errors || item.errors.length === 0) {
        return '';
    }

    let stateKey = buildProgressErrorAccordionKey(parentId, item, itemIndex);
    let expanded = isProgressErrorAccordionExpanded(stateKey);
    let accordionId = 'progress-errors-' + stateKey;
    let headingId = accordionId + '-heading';
    let collapseId = accordionId + '-collapse';
    let buttonCollapsedClass = expanded ? '' : ' collapsed';
    let collapseShowClass = expanded ? ' show' : '';
    let ariaExpanded = expanded ? 'true' : 'false';

    let html = '<div class="accordion progress-errors-accordion" id="' + accordionId + '" data-progress-errors-key="' + stateKey + '">' +
        '<div class="accordion-item progress-errors-item">' +
        '<h4 class="accordion-header" id="' + headingId + '">' +
        '<button class="accordion-button progress-errors-button' + buttonCollapsedClass + '" type="button"' +
        ' data-bs-toggle="collapse" data-bs-target="#' + collapseId + '"' +
        ' aria-expanded="' + ariaExpanded + '" aria-controls="' + collapseId + '">' +
        'Errors (' + item.errors.length + ')' +
        '</button>' +
        '</h4>' +
        '<div id="' + collapseId + '" class="accordion-collapse collapse' + collapseShowClass + '"' +
        ' aria-labelledby="' + headingId + '">' +
        '<div class="accordion-body progress-errors-body">' +
        '<ul class="mb-0">';

    $.each(item.errors, function(j, error) {
        html += '<li>' + escapeHtml(error) + '</li>';
    });

    html += '</ul>' +
        '</div>' +
        '</div>' +
        '</div>' +
        '</div>';

    return html;
}

function renderProgressItem(item, parentId, itemIndex) {
    let itemPercentComplete = getProgressItemPercentComplete(item);
    let itemLabel = safeTextValue(item.label, 'Progress Item') + ': ' + itemPercentComplete + '% Complete';

    if (item.message) {
        itemLabel += ' - ' + item.message;
    }

    let html = '<div class="progress-detail-item">' +
        renderProgressBar(itemPercentComplete, itemLabel, 'progress-detail-bar');

    if (item.errors && item.errors.length > 0) {
        html += renderProgressErrorsAccordion(item, parentId, itemIndex);
    }

    html += '</div>';

    return html;
}

function renderEndpointProgressGroup(endpointName, progressItems, endpointExpanded) {
    progressItems = Array.isArray(progressItems) ? progressItems : [];
    endpointExpanded = endpointExpanded === true;

    let endpointId = 'progress-endpoint-' + buildSafeProgressId(endpointName);
    let endpointPercentComplete = getProgressSummaryPercentComplete(progressItems);
    let endpointHasErrors = progressItems.some(function(item) {
        return item && item.errors && item.errors.length > 0;
    });
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

    progressItems.forEach(function(item, itemIndex) {
        html += renderProgressItem(item || {}, endpointId, itemIndex);
    });

    html += '</div>' +
        '</div>' +
        '</div>' +
        '</div>';

    return html;
}

$(document).on('shown.bs.collapse', '.progress-errors-accordion .accordion-collapse', function() {
    let stateKey = $(this).closest('.progress-errors-accordion').attr('data-progress-errors-key');
    if (stateKey) {
        progressErrorAccordionExpandedState[stateKey] = true;
    }
});

$(document).on('hidden.bs.collapse', '.progress-errors-accordion .accordion-collapse', function() {
    let stateKey = $(this).closest('.progress-errors-accordion').attr('data-progress-errors-key');
    if (stateKey) {
        progressErrorAccordionExpandedState[stateKey] = false;
    }
});

function renderProgressData(progressData, detailsExpanded) {
    detailsExpanded = detailsExpanded === true;

    if (!Array.isArray(progressData) || progressData.length === 0) {
        return '';
    }

    let endpointGroups = groupProgressByEndpoint(progressData);
    let summaryPercentComplete = getProgressSummaryPercentComplete(progressData);
    let hasErrors = progressData.some(function(item) {
        return item && item.errors && item.errors.length > 0;
    });
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
    let dataSetsHtml = $('#dataSets').html() || '';
    return dataSetsHtml.split(',').map(function(s) {
        return s.trim();
    });
}

function parseSseEventData(event, eventName) {
    try {
        return JSON.parse(event.data || '{}');
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'SSE event: ' + eventName);
        }
        return null;
    }
}

function initializeSSE(_callback) {
    if (!shouldContinueDatasetServerRequests()) {
        return;
    }

    if (eventSource !== null) {
        eventSource.close();
    }

    if (eventSourceFallbackInterval !== null) {
        clearInterval(eventSourceFallbackInterval);
        eventSourceFallbackInterval = null;
    }

    if (typeof EventSource !== 'function') {
        logWarn('EventSource is not available; live dataset updates are disabled.');
        return;
    }

    eventSource = new EventSource(getBasePath() + '/sse');

    eventSource.addEventListener("dataset-update", function(event) {
        try {
            if (!shouldContinueDatasetServerRequests()) {
                return;
            }

            let eventData = parseSseEventData(event, 'dataset-update');
            if (!eventData) {
                return;
            }

            if (getDataSets().indexOf(eventData.dataSet) !== -1) {
                logInfo("dataset-update: dataSet=" + eventData.dataSet + ", endpoint=" + eventData.endpoint);
                getUpdatedModels(_callback);
            }
        } catch (error) {
            reportAndRenderModelsError(error, 'SSE dataset-update handler');
        }
    });

    eventSource.addEventListener("endpoint-population-started", function(event) {
        try {
            let eventData = parseSseEventData(event, 'endpoint-population-started');
            if (!eventData) {
                return;
            }

            logInfo("endpoint-population-started: endpoint=" + eventData.endpoint);
        } catch (error) {
            reportAndRenderModelsError(error, 'SSE endpoint-population-started handler');
        }
    });

    eventSource.addEventListener("endpoint-population-complete", function(event) {
        try {
            let eventData = parseSseEventData(event, 'endpoint-population-complete');
            if (!eventData) {
                return;
            }

            logInfo("endpoint-population-complete: endpoint=" + eventData.endpoint);
        } catch (error) {
            reportAndRenderModelsError(error, 'SSE endpoint-population-complete handler');
        }
    });

    eventSource.addEventListener("all-complete", function(event) {
        try {
            logInfo("all-complete");
            $('#refresh').removeAttr('aria-disabled').removeClass('disabled').show();
        } catch (error) {
            reportAndRenderModelsError(error, 'SSE all-complete handler');
        }
    });

    eventSource.onerror = function() {
        logDebug('SSE connection closed or failed; live dataset updates are disabled for this page.');

        if (eventSource !== null) {
            eventSource.close();
            eventSource = null;
        }
    };
}

window.addEventListener("pagehide", function() {
    datasetPageUnloading = true;
    stopDatasetServerActivity();
});

window.addEventListener("pageshow", function(event) {
    if (!event.persisted) {
        return;
    }

    resumeDatasetServerActivity();

    if (!exists('#sessionEstablished') || $('#sessionEstablished').text() !== 'true') {
        return;
    }

    try {
        getUpdatedModels(renderModels);
        initializeSSE(renderModels);
    } catch (error) {
        reportAndRenderModelsError(error, 'dataset pageshow');
    }
});

function getUpdatedModels(_callback) {
    if (!shouldContinueDatasetServerRequests()) {
        return;
    }

    $.ajax({
        method: "POST",
        url: getBasePath() + '/models'
    }).done(function(models) {
        if (!shouldContinueDatasetServerRequests()) {
            return;
        }

        if (typeof _callback !== 'function') {
            reportAndRenderModelsError(new Error('Model callback is not a function'), getBasePath() + '/models callback');
            return;
        }

        try {
            _callback(models);
        } catch (error) {
            reportAndRenderModelsError(error, getBasePath() + '/models render callback');
        }
    }).fail(function(jqXHR, textStatus, errorThrown) {
        if (isRequestAborted(jqXHR, textStatus)) {
            return;
        }

        if (isServerUnavailableResponse(jqXHR)) {
            handleDatasetServerUnavailable(getBasePath() + '/models');
            return;
        }

        if (isUnauthorizedResponse(jqXHR)) {
            handleDatasetUnauthorized();
            return;
        }

        let message = getAjaxFailureMessage(jqXHR, textStatus, errorThrown, 'Unable to load records.');

        logError(
            'Unable to load models from ' + getBasePath() + '/models' +
            ' status=' + (jqXHR ? jqXHR.status : 'unknown') +
            ' textStatus=' + textStatus +
            ' errorThrown=' + errorThrown +
            ' message=' + message
        );

        $('#modelsContainer').html(
            '<div class="alert alert-danger" role="alert">' +
            'Unable to load records for this section. Please refresh the page or try again later.' +
            '</div>'
        );
    });
}

function renderNoModelsCompletedMessage() {
    return '<div class="alert alert-info" role="status">' +
        'No records were found for this section after the data load completed.' +
        '</div>';
}

function destroyCardCharts(container) {
    if (typeof Chart === 'undefined' || typeof Chart.getChart !== 'function') {
        return;
    }
    container.find('canvas.chart').each(function() {
        let chart = Chart.getChart(this);
        if (chart) {
            chart.destroy();
        }
    });
}

function rebuildSourceGroupedCards() {
    let container = $('#modelsContainer');
    sourceGroupedModels.forEach(function(model, id) {
        let selector = container.find('#' + id + '-selector');
        let panel = container.find('#' + id + '-panel');
        let card = buildCardData(model) || {};
        let selectorData = buildCardSelectorData(model);

        selector.replaceWith(renderCardSelectors(id, selectorData, card, selector.hasClass('active')));
        destroyCardCharts(panel);
        panel.html(renderCard(id, card));
    });

    let layout = container.find('.card-selector-layout');
    let sortButton = layout.find('.card-selector-sort-button[data-sort-direction]');
    if (sortButton.length > 0) {
        sortCardSelectors(layout, sortButton.attr('data-sort-index'), sortButton.attr('data-sort-direction'));
    }
}

function renderModels(models) {
    try {
        let currentFilter = $('#datasetFilter').val();
        if (currentFilter !== undefined) {
            datasetFilterQuery = currentFilter;
        }

        if (Array.isArray(models) && models.length > 0) {
            let items = [];
            sourceGroupedModels.clear();
            models.forEach(function(model, index) {
                model = model || {};

                if (model.allDataBySourceEndpointName && typeof buildCardSelectorData === 'function') {
                    sourceGroupedModels.set('card_' + (index + 1), model);
                }

                let card = buildCardData(model) || {};

                if (card.source === undefined || card.source === null) {
                    card.source = model.sourceEndpointName;
                }

                items.push({
                    selector: typeof buildCardSelectorData === 'function' ? buildCardSelectorData(model) : null,
                    card: card,
                    // The legend must retain disabled sources so they can be enabled again.
                    sources: model.allDataBySourceEndpointName ?
                        Object.keys(getAllDataBySourceEndpointName(model)) : card.source
                });
            });

            let sourceEndpointNames = [];
            items.forEach(function(item) {
                getSourceEndpointNames(item.sources).forEach(function(sourceEndpointName) {
                    if (sourceEndpointNames.indexOf(sourceEndpointName) === -1) {
                        sourceEndpointNames.push(sourceEndpointName);
                    }
                });
            });

            destroyCardCharts($('#modelsContainer'));
            if (typeof buildCardSelectorData === 'function') {
                $('#modelsContainer').html(renderCardSelectorLayout(items, sourceEndpointNames));
                applySourceEndpointVisibility();
                scheduleCardSelectorListResize();

            } else {
                let cards = [];
                items.forEach(function(item, index) {
                    cards.push(renderCard('card_' + (index + 1), item.card));
                });

                $('#modelsContainer').html(renderSourceEndpointLegend(sourceEndpointNames) + cards.join('\n'));
                applySourceEndpointVisibility();
            }

        } else {
            getCurrentProgress(function(progressData) {
                if (isAllProgressComplete(progressData)) {
                    $('#modelsContainer').html(renderNoModelsCompletedMessage());
                }
            });
        }
    } catch (error) {
        reportAndRenderModelsError(error, 'renderModels');
    }
}

$(document).on('input', '#datasetFilter', function() {
    try {
        datasetFilterQuery = $(this).val();
        applyDatasetFilter();
    } catch (error) {
        reportAndRenderModelsError(error, 'dataset filter input');
    }
});

$(document).on('click', '#modelsContainer .source-endpoint-legend-item[data-source-endpoint-interactive="true"]', function() {
    try {
        let sourceEndpointName = getSourceEndpointName($(this).attr('data-source-endpoint-name'));

        if (hiddenSourceEndpointNames.has(sourceEndpointName)) {
            hiddenSourceEndpointNames.delete(sourceEndpointName);
        } else {
            hiddenSourceEndpointNames.add(sourceEndpointName);
        }

        rebuildSourceGroupedCards();
        applySourceEndpointVisibility();

    } catch (error) {
        reportAndRenderModelsError(error, 'source endpoint legend click');
    }
});

function sortCardSelectors(layout, sortIndex, direction) {
    let list = layout.find('.card-selector-items');
    let sortedSelectors = list.children('.card-selector').get().sort(function(a, b) {
        let aValue = safeTextValue($(a).attr('data-sort-value-' + sortIndex), '');
        let bValue = safeTextValue($(b).attr('data-sort-value-' + sortIndex), '');
        let comparison = aValue.localeCompare(bValue, undefined, {
            numeric: true,
            sensitivity: 'base'
        });

        return direction === 'asc' ? comparison : -comparison;
    });
    list.append(sortedSelectors);
}

$(document).on('click', '#modelsContainer .card-selector-sort-button', function() {
    try {
        let button = $(this);
        let sortIndex = button.attr('data-sort-index');
        let layout = button.closest('.card-selector-layout');
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

        sortCardSelectors(layout, sortIndex, nextDirection);

    } catch (error) {
        reportAndRenderModelsError(error, 'card selector sort click');
    }
});

$(document).on('click', '#modelsContainer .card-selector', function() {
    try {
        let selector = $(this);
        let targetId = selector.attr('data-card-target');
        let container = $('#modelsContainer');

        container.find('.card-selector').removeClass('active').attr('aria-selected', 'false');
        selector.addClass('active').attr('aria-selected', 'true');
        container.find('.card-panel').addClass('d-none').attr('aria-hidden', 'true');
        container.find('#' + targetId).removeClass('d-none').removeAttr('aria-hidden');

        scheduleCardSelectorListResize();
    } catch (error) {
        reportAndRenderModelsError(error, 'card selector click');
    }
});

$(window).on('resize', function() {
    try {
        scheduleCardSelectorListResize();
    } catch (error) {
        reportAndRenderModelsError(error, 'window resize');
    }
});

$(document).ready(function() {
    try {
        if (!exists('#sessionEstablished')) {
            return;
        }
        if ($('#sessionEstablished').text() !== 'true') {
            return;
        }

        getCurrentProgress(function(progressData) {
            if (!shouldContinueDatasetServerRequests()) {
                return;
            }

            if (progressData) {
                if ( ! isAllProgressComplete(progressData) ) {
                    let el = $('#progressContainer');
                    $(el).html(renderProgressData(progressData));
                    $(el).removeClass('hidden');
                    progressRefreshTimer = setTimeout(refreshProgress, 5000);
                }
            }
        });
    } catch (error) {
        reportAndRenderModelsError(error, 'dataset document ready');
    }
});