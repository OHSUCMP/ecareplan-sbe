// Chart sizing is centralized here so the dimensions can be changed without
// having to update each chart or its template.
const LINE_CHART_MIN_WIDTH = 500;
const LINE_CHART_MAX_WIDTH = 800;
const LINE_CHART_ASPECT_RATIO = 2;

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
