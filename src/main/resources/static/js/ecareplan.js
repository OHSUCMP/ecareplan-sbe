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
