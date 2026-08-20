// Chart sizing is centralized here so the dimensions can be changed without
// having to update each chart or its template.
const LINE_CHART_MIN_WIDTH = 100;
const LINE_CHART_MAX_WIDTH = 800;
const LINE_CHART_ASPECT_RATIO = 2;

function renderCardChartContainer(baseId, chartData) {
    if (!chartData || !chartData.data || chartData.data.length === 0) return '';

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

        if (!chartData.datasets[0].data || chartData.datasets[0].data.length === 0) return;

        createLineChart(
            Object.assign(getChartOptions(chart), { canvas: canvas }),
            chartData
        );
    });
}

function getChartData(chart) {
    const points = (chart && chart.data ? chart.data : [])
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

    return {
        datasets: [
            {
                data: points,
                borderColor: 'rgb(48, 96, 128)',
                fill: false
            }
        ]
    };
}

function getChartOptions(chart) {
    const title = chart && chart.title ? chart.title : '';
    const yAxisTitle = chart && chart.labels && chart.labels.y ? chart.labels.y : '';

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
                display: false
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