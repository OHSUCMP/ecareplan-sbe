(function() {
    const FRONT_END_EXCEPTION_AUDIT_PATH = '/front-end-exception/audit';
    const MAX_REPORTED_STACK_LINES = 4;
    const reportedFrontEndExceptionSignatures = {};
    let reportingFrontEndException = false;

    function getFrontEndExceptionPageUrl() {
        if (!window.location) {
            return '';
        }

        return window.location.pathname + window.location.search + window.location.hash;
    }

    function getContextPathForFrontEndExceptionHandler() {
        let contextMeta = document.querySelector('meta[name="ctx"]');
        let contextPath = contextMeta ? contextMeta.getAttribute('content') : '/';

        if (!contextPath) {
            return '';
        }

        return contextPath.replace(/\/$/, '');
    }

    function buildFrontEndExceptionAuditUrl() {
        return getContextPathForFrontEndExceptionHandler() + FRONT_END_EXCEPTION_AUDIT_PATH;
    }

    function getFrontEndExceptionType(error) {
        if (error && error.name) {
            return String(error.name);
        }

        if (error && error.constructor && error.constructor.name) {
            return String(error.constructor.name);
        }

        return 'FrontEndException';
    }

    function getFrontEndExceptionMessage(error) {
        if (error === undefined || error === null) {
            return 'An unknown front-end error occurred.';
        }

        if (typeof error === 'string') {
            return error;
        }

        if (error.message) {
            return String(error.message);
        }

        try {
            return JSON.stringify(error);
        } catch (serializationError) {
            return String(error);
        }
    }

    function normalizeFrontEndStackLine(line) {
        return String(line || '').trim();
    }

    function isUsefulProjectJavaScriptStackLine(line) {
        let normalizedLine = normalizeFrontEndStackLine(line);

        if (normalizedLine.length === 0) {
            return false;
        }

        if (normalizedLine.indexOf('/webjars/') !== -1) {
            return false;
        }

        if (normalizedLine.indexOf('/js/fhir-client-') !== -1) {
            return false;
        }

        if (normalizedLine.indexOf('/js/') !== -1) {
            return true;
        }

        if (window.location && window.location.origin &&
            normalizedLine.indexOf(window.location.origin) !== -1 &&
            normalizedLine.indexOf('/front-end-exception/audit') === -1) {
            return true;
        }

        return false;
    }

    function getFilteredFrontEndStackTrace(error, fallbackSource, fallbackLine, fallbackColumn) {
        let lines = [];

        if (error && error.stack) {
            lines = String(error.stack)
                .split('\n')
                .map(normalizeFrontEndStackLine)
                .filter(isUsefulProjectJavaScriptStackLine);
        }

        if (lines.length === 0 && fallbackSource) {
            let sourceLine = String(fallbackSource);

            if (fallbackLine) {
                sourceLine += ':' + fallbackLine;
            }

            if (fallbackColumn) {
                sourceLine += ':' + fallbackColumn;
            }

            if (isUsefulProjectJavaScriptStackLine(sourceLine)) {
                lines.push(sourceLine);
            }
        }

        return lines.slice(0, MAX_REPORTED_STACK_LINES).join('\n');
    }

    function buildFrontEndExceptionPayload(error, fallbackSource, fallbackLine, fallbackColumn) {
        return {
            pageUrl: getFrontEndExceptionPageUrl(),
            type: getFrontEndExceptionType(error),
            message: getFrontEndExceptionMessage(error),
            stackTrace: getFilteredFrontEndStackTrace(error, fallbackSource, fallbackLine, fallbackColumn)
        };
    }

    function shouldReportFrontEndException(payload) {
        let signature = payload.pageUrl + '|' + payload.type + '|' + payload.message + '|' + payload.stackTrace;

        if (reportedFrontEndExceptionSignatures[signature]) {
            return false;
        }

        reportedFrontEndExceptionSignatures[signature] = true;
        return true;
    }

    function logFrontEndExceptionLocally(payload, originalError) {
        if (!window.console || typeof window.console.error !== 'function') {
            return;
        }

        console.error('Handled front-end exception:', payload, originalError || '');
    }

    function reportFrontEndException(payload) {
        if (reportingFrontEndException || !shouldReportFrontEndException(payload)) {
            return;
        }

        reportingFrontEndException = true;

        try {
            let xhr = new XMLHttpRequest();
            xhr.open('POST', buildFrontEndExceptionAuditUrl(), true);
            xhr.setRequestHeader('Content-Type', 'application/json;charset=UTF-8');
            xhr.onreadystatechange = function() {
                if (xhr.readyState === 4) {
                    reportingFrontEndException = false;
                }
            };
            xhr.send(JSON.stringify(payload));
        } catch (reportingError) {
            reportingFrontEndException = false;

            if (window.console && typeof window.console.warn === 'function') {
                console.warn('Unable to report front-end exception:', reportingError);
            }
        }
    }

    window.handleFrontEndException = function(error, fallbackSource, fallbackLine, fallbackColumn) {
        let payload = buildFrontEndExceptionPayload(error, fallbackSource, fallbackLine, fallbackColumn);

        logFrontEndExceptionLocally(payload, error);
        reportFrontEndException(payload);

        return payload;
    };

    window.runWithFrontEndExceptionHandling = function(callback, context) {
        try {
            return callback();
        } catch (error) {
            window.handleFrontEndException(error, context);
            return null;
        }
    };

    window.addEventListener('error', function(event) {
        window.handleFrontEndException(
            event.error || event.message,
            event.filename,
            event.lineno,
            event.colno
        );
    });

    window.addEventListener('unhandledrejection', function(event) {
        window.handleFrontEndException(event.reason || 'Unhandled promise rejection');
    });
})();

function exists(el) {
    // see https://learn.jquery.com/using-jquery-core/faq/how-do-i-test-whether-an-element-exists/
    return $(el).length;
}

function formatDate(date, format = 'MMM d, yyyy') {
    if (date === undefined || date === null) {
        return '';
    }

    let parsedDate;
    if (typeof date === 'number' || typeof date === 'string') {
        parsedDate = new Date(date);
    } else if (date instanceof Date) {
        parsedDate = date;
    }

    if (!parsedDate || isNaN(parsedDate.getTime())) {
        return '';
    }

    const monthsShort = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
    const monthsLong = [
        'January', 'February', 'March', 'April', 'May', 'June',
        'July', 'August', 'September', 'October', 'November', 'December'
    ];

    const values = {
        yyyy: parsedDate.getFullYear(),
        yy: String(parsedDate.getFullYear()).slice(-2),
        MMMM: monthsLong[parsedDate.getMonth()],
        MMM: monthsShort[parsedDate.getMonth()],
        MM: String(parsedDate.getMonth() + 1).padStart(2, '0'),
        M: parsedDate.getMonth() + 1,
        dd: String(parsedDate.getDate()).padStart(2, '0'),
        d: parsedDate.getDate()
    };

    return format.replace(/yyyy|MMMM|MMM|MM|M|dd|d/g, function(token) {
        return values[token];
    });
}
