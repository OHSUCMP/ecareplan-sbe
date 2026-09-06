// Run with: node --test src/test/js/source-filtered-cards.test.cjs
const assert = require('node:assert/strict');
const fs = require('node:fs');
const path = require('node:path');
const vm = require('node:vm');
const test = require('node:test');

const resources = path.resolve(__dirname, '../../main/resources');

function loadPage(page) {
    // DOM behavior is exercised separately in a browser; ready callbacks must not start server requests here.
    const context = vm.createContext({
        document: {},
        window: { addEventListener() {} },
        $: () => ({ on() {}, ready() {} })
    });
    for (const file of ['ecareplan.js', 'endpoint.js', 'dataset.js', 'chart.js']) {
        vm.runInContext(fs.readFileSync(path.join(resources, 'static/js', file), 'utf8'), context, { filename: file });
    }
    const template = fs.readFileSync(path.join(resources, 'templates/patient', page + '.mustache'), 'utf8');
    vm.runInContext(template.match(/<script type="text\/javascript">([\s\S]*?)<\/script>/)[1], context);
    context.setHidden = (...sources) => {
        context.hidden = sources;
        vm.runInContext('hiddenSourceEndpointNames = new Set(hidden)', context);
    };
    return context;
}

function record(source, day) {
    return {
        sourceEndpointName: source,
        description: 'Result from ' + source,
        effectiveDate: '2026-09-0' + day + 'T12:00:00Z',
        authored: '2026-09-0' + day + 'T12:00:00Z',
        resultText: source + day,
        resultUnits: source + ' units',
        resultValue: { components: [{ conceptName: 'Measurement', value: day }] },
        performers: [source + ' performer'],
        notes: [source + ' note'],
        referenceRange: source + ' range',
        score: day,
        interpretation: source + ' interpretation'
    };
}

function model() {
    return {
        label: 'Assessment',
        learnMoreUrl: 'https://example.org/assessment',
        // Deliberately put Y first: object key order must not determine the primary record.
        allDataBySourceEndpointName: { Y: [record('Y', 3), record('Y', 2), record('Y', 1)], X: [record('X', 4)] }
    };
}

function plain(value) {
    return JSON.parse(JSON.stringify(value));
}

for (const page of ['lab-results', 'vitals']) {
    test(page + ': promotes Y, removes all X details, and restores X without mutating data', () => {
        const context = loadPage(page);
        const input = model();
        const snapshot = JSON.stringify(input);
        const original = plain(context.buildCardData(input));
        assert.deepEqual(original.source, ['X', 'Y']);
        assert.equal(original.rows[0][0].value, 'X4');

        context.setHidden('X');
        const filtered = plain(context.buildCardData(input));
        assert.deepEqual(filtered.source, ['Y']);
        assert.equal(filtered.title, 'Result from Y');
        assert.equal(filtered.rows[0][0].value, 'Y3');
        assert.deepEqual(filtered.rows.find(row => row.title === 'Historical Data').rows.map(row => row.data[0]), ['Y2', 'Y1']);
        assert.equal(JSON.stringify(filtered).includes('X'), false);
        assert.equal(filtered.chart.labels.y, 'Y units');
        assert.equal(filtered.chart.lines[0].data.length, 3);
        assert.equal(context.buildCardSelectorData(input)[1].valueForCompare, record('Y', 3).effectiveDate);

        context.setHidden('X', 'Y');
        assert.deepEqual(plain(context.buildCardData(input).source), []);
        assert.equal(context.buildCardData(input).chart, null);
        context.setHidden();
        assert.deepEqual(plain(context.buildCardData(input)), original);
        assert.equal(JSON.stringify(input), snapshot);
    });

    test(page + ': one enabled record has no history and no duplicate chart points', () => {
        const context = loadPage(page);
        context.setHidden('Y');
        const card = context.buildCardData(model());
        assert.equal(card.rows.some(row => row.title === 'Historical Data'), false);
        assert.equal(card.chart.lines[0].data.length, 1);
    });
}

test('assessments: filters history, chart, tint and selector date, then restores all responses', () => {
    const context = loadPage('assessments');
    const input = model();
    const original = plain(context.buildCardData(input));
    assert.deepEqual(original.source, ['X', 'Y']);
    assert.deepEqual(original.rows[0].rows.map(row => row.data[1]), [4, 3, 2, 1]);
    context.setHidden('X');
    const filtered = plain(context.buildCardData(input));
    assert.deepEqual(filtered.source, ['Y']);
    assert.deepEqual(filtered.rows[0].rows.map(row => row.data[1]), [3, 2, 1]);
    assert.equal(filtered.chart.data.length, 3);
    assert.equal(context.buildCardSelectorData(input)[1].valueForCompare, record('Y', 3).authored);
    assert.equal(filtered.learnMoreUrl, input.learnMoreUrl);
    context.setHidden('Y');
    assert.equal(context.buildCardData(input).chart, undefined);
    context.setHidden('X', 'Y');
    assert.deepEqual(plain(context.buildCardData(input).rows), []);
    context.setHidden();
    assert.deepEqual(plain(context.buildCardData(input)), original);
});

test('merges interleaved dates, treats epoch zero as valid, and puts invalid dates last with deterministic ties', () => {
    const context = loadPage('vitals');
    const input = { allDataBySourceEndpointName: {
        X: [{ id: 'new', effectiveDate: '2026-09-06T01:00:00+0100' }, { id: 'old', effectiveDate: 0 }, { id: 'missing' }],
        Y: [{ id: 'tie', effectiveDate: '2026-09-06T00:00:00Z' }, { id: 'middle', effectiveDate: '2025-01-01' }, { id: 'invalid', effectiveDate: 'invalid' }]
    } };
    assert.deepEqual(plain(context.getVisibleSourceRecords(input, 'effectiveDate').map(item => item.id)),
        ['new', 'tie', 'middle', 'old', 'missing', 'invalid']);
    assert.equal(context.getVisibleSourceRecords(input, 'effectiveDate')[0].effectiveDate, '2026-09-06T01:00:00+01:00');
    assert.equal(input.allDataBySourceEndpointName.X[0].effectiveDate, '2026-09-06T01:00:00+0100');
});

test('guards malformed groups and records and uses own source keys safely', () => {
    const context = loadPage('vitals');
    const groups = JSON.parse('{"__proto__":[{"resultText":"safe"}],"constructor":[{}]," X ":[{}]," ":[{}]}');
    groups.bad = 'not a list';
    groups.nulls = [null, false, 3, 'text', []];
    Object.setPrototypeOf(groups, { inherited: [record('inherited', 6)] });
    const input = { allDataBySourceEndpointName: groups };
    assert.deepEqual(plain(context.getVisibleSourceRecords(input, 'effectiveDate').map(item => item.sourceEndpointName)),
        ['__proto__', 'constructor', 'X', 'Unknown Source']);
    context.setHidden('__proto__', 'constructor', 'X', 'Unknown Source');
    assert.deepEqual(plain(context.getVisibleSourceRecords(input, 'effectiveDate')), []);
    for (const value of [undefined, null, {}, { allDataBySourceEndpointName: [] }, { allDataBySourceEndpointName: 'bad' }]) {
        assert.deepEqual(plain(context.getVisibleSourceRecords(value, 'effectiveDate')), []);
    }
});

test('all three pages tolerate absent and malformed models', () => {
    for (const page of ['assessments', 'vitals', 'lab-results']) {
        const context = loadPage(page);
        for (const input of [undefined, null, {}, { allDataBySourceEndpointName: { X: [null, []] } }]) {
            assert.deepEqual(plain(context.buildCardData(input).source), []);
            assert.equal(context.buildCardSelectorData(input).length, 2);
        }
    }
});
