// Chart sizing is centralized here so the dimensions can be changed without
// having to update each chart or its template.
const LINE_CHART_MIN_WIDTH = 100;
const LINE_CHART_MAX_WIDTH = 800;
const LINE_CHART_ASPECT_RATIO = 2;
const LINE_CHART_COLORS = [
    'rgb(48, 96, 128)',
    'rgb(192, 80, 77)',
    'rgb(79, 129, 189)',
    'rgb(155, 187, 89)',
    'rgb(128, 100, 162)',
    'rgb(75, 172, 198)',
    'rgb(247, 150, 70)'
];

function chartHasData(chartData) {
    if (!chartData) return false;

    if (Array.isArray(chartData.data) && chartData.data.length > 0) {
        return true;
    }

    return Array.isArray(chartData.lines)
        && chartData.lines.some(function(line) {
            return line && Array.isArray(line.data) && line.data.length > 0;
        });
}

function renderCardChartContainer(baseId, chartData) {
    if (!chartHasData(chartData)) return '';

    return '<div class="chart-container mb-3">' +
        '<canvas class="chart" id="' + baseId + '-chart" data-chart-id="' + baseId + '" aria-label="' + (chartData.title ?? 'Chart') + '"></canvas>' +
        '<script type="application/json" id="' + baseId + '-chart-data">' +
        JSON.stringify(chartData) +
        '</script>' +
        '</div>';
}

function createLineChart(options, data) {
    if (typeof Chart === 'undefined') {
        throw new Error('Chart.js is not loaded');
    }

    if (!options || !options.canvas) {
        throw new Error('A canvas element is required to create a line chart');
    }

    const canvas = options.canvas && options.canvas.nodeType === 1
        ? options.canvas
        : document.querySelector(options.canvas);

    if (!canvas || typeof canvas.getContext !== 'function') {
        throw new Error('The chart canvas could not be found');
    }

    const chartData = data || { labels: [], datasets: [] };
    const container = canvas.parentElement;
    if (container) {
        container.style.minWidth = LINE_CHART_MIN_WIDTH + 'px';
        container.style.maxWidth = LINE_CHART_MAX_WIDTH + 'px';
        container.style.aspectRatio = LINE_CHART_ASPECT_RATIO + '';
    }

    // `canvas` is an instruction for this helper, not a Chart.js option.
    const chartOptions = Object.assign({
        responsive: true,
        // The container owns the aspect ratio. This gives Chart.js an
        // explicit height to recalculate whenever the container grows.
        maintainAspectRatio: false
    }, options);
    delete chartOptions.canvas;
    chartOptions.maintainAspectRatio = false;

    // Remove Chart.js' default horizontal padding so the first and last data
    // points sit at the edges of the plotting area.
    const xValues = (chartData.datasets || [])
        .flatMap((dataset) => dataset.data || [])
        .map((point) => typeof point === 'object' ? Number(point.x) : NaN)
        .filter((value) => Number.isFinite(value));
    if (xValues.length > 1) {
        const xScale = Object.assign({}, chartOptions.scales && chartOptions.scales.x);
        if (xScale.min === undefined) {
            xScale.min = Math.min(...xValues);
        }
        if (xScale.max === undefined) {
            xScale.max = Math.max(...xValues);
        }
        xScale.offset = false;
        chartOptions.scales = Object.assign({}, chartOptions.scales, { x: xScale });
    }

    return new Chart(canvas.getContext('2d'), {
        type: 'line',
        data: chartData,
        options: chartOptions
    });
}

function formatChartDate(timestamp) {
    const date = new Date(Number(timestamp));
    if (Number.isNaN(date.getTime())) {
        return '';
    }

    return (date.getMonth() + 1) + '/' + date.getDate() + '/'
        + String(date.getFullYear()).slice(-2);
}

function renderCharts() {
    $('.chart').each(function() {
        const canvas = this;
        const chartId = $(canvas).attr('data-chart-id');
        const chartDataElement = $('#' + chartId + '-chart-data')[0];

        if (!chartDataElement) return;

        const chart = JSON.parse(chartDataElement.textContent || '{}');
        const chartData = getChartData(chart);

        if (!chartData.datasets || !chartData.datasets.some(function(dataset) {
            return dataset.data && dataset.data.length > 0;
        })) return;

        createLineChart(
            Object.assign(getChartOptions(chart), { canvas: canvas }),
            chartData
        );
    });
}

function normalizeChartPoints(points) {
    return (points || [])
        .map(function(point) {
            return {
                x: new Date(point.x).getTime(),
                y: Number(point.y)
            };
        })
        .filter(function(point) {
            return Number.isFinite(point.x) && Number.isFinite(point.y);
        })
        .sort(function(a, b) {
            return a.x - b.x;
        });
}

function getChartData(chart) {
    if (chart && Array.isArray(chart.lines) && chart.lines.length > 0) {
        return {
            datasets: chart.lines
                .map(function(line, index) {
                    return {
                        label: line.label ?? '',
                        data: normalizeChartPoints(line.data),
                        borderColor: LINE_CHART_COLORS[index % LINE_CHART_COLORS.length],
                        backgroundColor: LINE_CHART_COLORS[index % LINE_CHART_COLORS.length],
                        fill: false
                    };
                })
                .filter(function(dataset) {
                    return dataset.data.length > 0;
                })
        };
    }

    const points = normalizeChartPoints(chart && chart.data ? chart.data : []);

    return {
        datasets: [
            {
                data: points,
                borderColor: LINE_CHART_COLORS[0],
                backgroundColor: LINE_CHART_COLORS[0],
                fill: false
            }
        ]
    };
}

function getChartOptions(chart) {
    const title = chart && chart.title ? chart.title : '';
    const yAxisTitle = chart && chart.labels && chart.labels.y ? chart.labels.y : '';
    const hasMultipleLines = chart && Array.isArray(chart.lines) && chart.lines.length > 1;

    return {
        responsive: true,
        maintainAspectRatio: false,
        elements: {
            line: {
                tension: 0
            }
        },
        plugins: {
            title: {
                display: title.length > 0,
                text: title
            },
            legend: {
                display: hasMultipleLines
            },
            tooltip: {
                callbacks: {
                    title: function(items) {
                        return items.length > 0
                            ? formatChartDate(items[0].parsed.x)
                            : '';
                    }
                }
            }
        },
        scales: {
            x: {
                type: 'linear',
                title: {
                    display: true,
                    text: 'Date'
                },
                ticks: {
                    autoSkip: true,
                    maxTicksLimit: 7,
                    callback: function(value) {
                        return formatChartDate(value);
                    }
                },
                grid: {
                    offset: false
                }
            },
            y: {
                beginAtZero: false,
                title: {
                    display: yAxisTitle.length > 0,
                    text: yAxisTitle
                }
            }
        }
    };
}

function buildConsolidatedChartData(model) {
    if (!model || !model.mostRecentData) return null;

    const records = [ model.mostRecentData ].concat(model.historicalData || []);
    const linesByConceptName = new Map();

    records.forEach(function(record) {
        if (!record || !record.effectiveDate || !record.resultValue || !Array.isArray(record.resultValue.components)) {
            return;
        }

        record.resultValue.components.forEach(function(component) {
            if (!component || !component.conceptName) return;

            const y = Number(component.value);
            if (!Number.isFinite(y)) return;

            if (!linesByConceptName.has(component.conceptName)) {
                linesByConceptName.set(component.conceptName, []);
            }

            linesByConceptName.get(component.conceptName).push({
                x: record.effectiveDate,
                y: y
            });
        });
    });

    const lines = Array.from(linesByConceptName.entries())
        .map(function(entry) {
            return {
                label: entry[0],
                data: entry[1]
            };
        })
        .filter(function(line) {
            return line.data.length > 0;
        });

    if (lines.length === 0) return null;

    return {
        title: model.mostRecentData.description + ' Over Time',
        labels: {
            x: 'Date',
            y: model.mostRecentData.resultUnits ?? ''
        },
        lines: lines
    };
}
