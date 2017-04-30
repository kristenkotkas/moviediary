var log = console.log.bind(console);
var loadingCounter = 0;
var types = {
    today: 1,
    week: 2,
    month: 3,
    year: 4,
    all: 5,
    search: 6
};
function removeLoader() {
    document.getElementsByTagName('body')[0].className += " loaded";
}

function asyncLoadCSS(href) {
    loadingCounter++;
    var ss = document.createElement('link');
    ss.href = href;
    ss.rel = 'stylesheet';
    ss.type = 'text/css';
    ss.media = 'bogus'; //fetch without blocking
    ss.onload = function () {
        ss.media = 'screen'; //render after loaded
        loadingCounter--;
        if (loadingCounter === 0) {
            removeLoader();
        }
    };
    document.getElementsByTagName('head')[0].appendChild(ss);
}

if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
        navigator.serviceWorker
            .register('/static/service-worker.js', {scope: '/static/'})
            .then(function () {
                log('Service worker registered');
            })
            .catch(function (err) {
                log('Error registering service worker: ' + err);
            });
    });
} else {
    log("service workers not supported")
}

asyncLoadCSS('/static/css/font-awesome/css/font-awesome.min.css');
asyncLoadCSS('/static/css/custom/materialize.min.css');
asyncLoadCSS('/static/css/custom/base.css');

var tagsToReplace = {
    '&': '&amp;',
    '<': '&lt;',
    '>': '&gt;'
};

function replaceTag(tag) {
    return tagsToReplace[tag] || tag;
}

function safe_tags_replace(str) {
    return str.replace(/[&<>]/g, replaceTag);
}

function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length === 2) return parts.pop().split(";").shift();
}

function getMonth(start, lang) {
    var startArray = start.split(' ');
    var month = startArray[1];
    return startArray[0] + lang[month.toUpperCase()] + ' ' + startArray[2];
}

function getUrlParam(param) {
    location.search.substr(1)
        .split("&")
        .some(function (item) { // returns first occurence and stops
            return item.split("=")[0] === param && (param = item.split("=")[1]);
        });
    return param;
}

function isNormalInteger(str) {
    var n = Math.floor(Number(str));
    return String(n) === str && n >= 0;
}

//History time interval

function getThisWeek() {
    var result = {};
    var curr = new Date(); // get current date
    var first = curr.getDate() - ((curr.getDay() + 6) % 7); // First day is the day of the month - the day of the week
    var firstday = new Date(curr.setDate(first));
    var lastday = new Date(curr.setDate(curr.getDate() + 6));
    result['start'] = firstday;
    result['end'] = lastday;
    return result;
}

function getThisMonth(index) {
    var result = {};
    var date = new Date();
    var firstday = new Date(date.getFullYear(), date.getMonth() + index, 1);
    var lastday = new Date(date.getFullYear(), date.getMonth() + 1 + index, 0);
    result['start'] = firstday;
    result['end'] = lastday;
    return result;
}

function getThisYear() {
    var result = {};
    var date = new Date();
    var firstday = new Date(date.getFullYear(), 0, 1);
    var lastday = new Date(date.getFullYear(), 11, 31);
    result['start'] = firstday;
    result['end'] = lastday;
    return result;
}

function getYear(year) {
    var result = {};
    var firstday = new Date(year, 0, 1);
    var lastday = new Date(year, 11, 31);
    result['start'] = firstday;
    result['end'] = lastday;
    return result;
}

function searchYear(year, lang, startDateField, endDateField) {
    var dates = getYear(year);
    var monthIndex = -1 * ((new Date().getFullYear() - year) * 12 + (new Date().getMonth()));
    console.log(dates);
    startDateField.pickadate('picker').set('select', dates['start']);
    endDateField.pickadate('picker').set('select', dates['end']);
    makeHistory(eventbus, lang, startDateField.val(), endDateField.val(), monthIndex);
}

function makeDropList(start, lang, type, startDateField, endDateField) {
    var array = [];
    for (var year = start; year <= new Date().getFullYear(); year++) {
        array.push(year);
    }

    $.each(array, function (i) {
        yearDropdown.append(
            $.parseHTML(
                '<li id="' + type + '-drop-' + array[i] + '"><a>' + array[i] + '</a></li>'
            )
        );

        var yearButton = document.getElementById(type + '-drop-' + array[i]);
        yearButton.onclick = function () {
            searchYear(array[i], lang, startDateField, endDateField);
        }
    });
}

function minutesToString(minutes) {
    var seconds = minutes * 60;
    var daysString = 'day';
    var hoursString = 'h';
    var minString = 'min';
    var daysStringPlur = 'days';
    var hoursStringPlur = 'h';
    var minStringPlur = 'min';
    var result = '';

    var numdays = Math.floor(seconds / 86400);
    var numhours = Math.floor((seconds % 86400) / 3600);
    var numminutes = Math.floor(((seconds % 86400) % 3600) / 60);

    if (numdays > 0) {
        if (numdays > 1) {
            result += numdays + ' ' + daysStringPlur + ' ';
        } else {
            result += numdays + ' ' + daysString + ' ';
        }
    }

    if (numhours > 0) {
        if (numhours > 1) {
            result += numhours + ' ' + hoursStringPlur + ' ';
        } else {
            result += numhours + ' ' + hoursString + ' ';
        }
    }

    if (numminutes > 0) {
        if (numminutes > 1) {
            result += numminutes + ' ' + minStringPlur + ' ';
        } else {
            result += numminutes + ' ' + minString + ' ';
        }
    }

    if (result.length > 0) {
        return result;
    } else {
        return '0 ' + minString;
    }
}

function replaceUrlParameter(param, value) {
    var url = location.href;
    var splitted = url.split('?');
    var shouldPushHistory = true;
    if (splitted.length > 1) {
        url = splitted[0];
        if (splitted[1].indexOf(value) !== -1) {
            shouldPushHistory = false;
        }
    }
    url = url + "?" + param + "=" + value;
    if (shouldPushHistory) {
        window.history.pushState('', '', url);
    }
}