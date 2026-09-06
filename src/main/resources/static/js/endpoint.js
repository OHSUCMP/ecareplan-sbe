const sourceEndpointColorPalette = [
    { background: '#eef7ff', border: '#9dccf0', accent: '#2f80c0' },
    { background: '#f0fbf6', border: '#9fdcbd', accent: '#2e8b57' },
    { background: '#fff7e8', border: '#f0c57a', accent: '#b7791f' },
    { background: '#f7f0ff', border: '#c8a7ed', accent: '#805ad5' },
    { background: '#fff0f5', border: '#eda8c5', accent: '#c0567a' },
    { background: '#eefcfb', border: '#8fd8d3', accent: '#2c8f8a' },
    { background: '#f8f9e8', border: '#d9dc8f', accent: '#7a7f22' },
    { background: '#f3f4ff', border: '#aeb8f0', accent: '#4c5fc0' },
    { background: '#fff4ed', border: '#f0a879', accent: '#c45a2f' },
    { background: '#effaf0', border: '#93d59b', accent: '#3b8f43' },
    { background: '#f1f7ff', border: '#8fb8e8', accent: '#3366aa' },
    { background: '#fffbed', border: '#ead26f', accent: '#9a7a00' },
    { background: '#f6f1ff', border: '#b99fe6', accent: '#6b46c1' },
    { background: '#eef9f3', border: '#87cfa5', accent: '#237a4f' },
    { background: '#fff0f0', border: '#e8a0a0', accent: '#b64040' },
    { background: '#eef6f7', border: '#8fc8cf', accent: '#2f7f8f' }
];

const defaultSourceEndpointColor = { background: '#f8fbfc', border: '#dbe8ee', accent: '#7698a1' };

function getSourceEndpointColorPaletteIndex(sourceEndpointName) {
    let source = String(safeTextValue(sourceEndpointName, '')).trim().toLowerCase();

    let hash = 2166136261;
    for (let i = 0; i < source.length; i++) {
        hash ^= source.charCodeAt(i);
        hash = Math.imul(hash, 16777619);
    }

    return (hash >>> 0) % sourceEndpointColorPalette.length;
}

function getSourceEndpointColor(sourceEndpointName) {
    let source = String(safeTextValue(sourceEndpointName, '')).trim();

    if (source === '') {
        return defaultSourceEndpointColor;
    }

    return sourceEndpointColorPalette[getSourceEndpointColorPaletteIndex(source)];
}

function renderSourceEndpointStyle(sourceEndpointName) {
    let color = getSourceEndpointColor(sourceEndpointName);

    return ' style="' +
        '--source-endpoint-bg: ' + escapeHtml(color.background) + '; ' +
        '--source-endpoint-border: ' + escapeHtml(color.border) + '; ' +
        '--source-endpoint-accent: ' + escapeHtml(color.accent) + ';"';
}

function getSourceEndpointName(value) {
    let source = String(safeTextValue(value, '')).trim();
    return source === '' ? 'Unknown Source' : source;
}

function getSourceEndpointNames(source) {
    let sources = Array.isArray(source) ? source : [source];
    let uniqueSources = [];

    sources
        .map(getSourceEndpointName)
        .filter(function(sourceEndpointName) {
            return sourceEndpointName;
        })
        .forEach(function(sourceEndpointName) {
            if (uniqueSources.indexOf(sourceEndpointName) === -1) {
                uniqueSources.push(sourceEndpointName);
            }
        });

    return uniqueSources;
}

function getPrimarySourceEndpointName(source) {
    let sourceNames = getSourceEndpointNames(source);
    return safeTextValue(sourceNames[0], getSourceEndpointName(null));
}

function getSourceEndpointDataAttribute(source) {
    return getSourceEndpointNames(source).join('|');
}

function applyEndpointTinting(container) {
    let root = container ? $(container) : $(document);

    root.find('[data-endpoint-name]').each(function() {
        let element = $(this);
        let sourceEndpointName = getSourceEndpointName(element.attr('data-endpoint-name'));
        let color = getSourceEndpointColor(sourceEndpointName);

        element.addClass('source-endpoint-tinted');
        element.css('--source-endpoint-bg', color.background);
        element.css('--source-endpoint-border', color.border);
        element.css('--source-endpoint-accent', color.accent);
    });
}

$(document).ready(function() {
    try {
        applyEndpointTinting();
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'endpoint document ready');
        } else {
            logError('Endpoint rendering error: ' + error);
        }
    }
});

window.addEventListener('pageshow', function() {
    try {
        applyEndpointTinting();
    } catch (error) {
        if (typeof window.handleFrontEndException === 'function') {
            window.handleFrontEndException(error, 'endpoint pageshow');
        } else {
            logError('Endpoint pageshow error: ' + error);
        }
    }
});