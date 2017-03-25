fallback.load({
    'font-awesome_css': [
        '//cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css',
        '//movies.kyngas.eu/static/css/font-awesome-4.7.0/css/font-awesome.min.css',
        '//localhost:8081/static/css/font-awesome-4.7.0/css/font-awesome.min.css'
    ],
    materialize_css: [
        //'//movies.kyngas.eu/static/css/custom/materialize.css',
        '//localhost:8081/static/css/custom/materialize.css'
    ],
    base_css: [
        //'//movies.kyngas.eu/static/css/custom/base.css',
        '//localhost:8081/static/css/custom/base.css'
    ],
    jQuery: [
        '//cdnjs.cloudflare.com/ajax/libs/jquery/3.1.1/jquery.min.js',
        '//movies.kyngas.eu/static/js/jquery.min.js',
        '//localhost:8081/static/js/jquery.min.js'
    ],
    Materialize: [
        //'//movies.kyngas.eu/static/js/custom/materialize.js',
        '//localhost:8081/static/js/custom/materialize.js'
    ],
    ClockPicker_css: [
        //'//movies.kyngas.eu/static/css/clockpicker.css',
        '//localhost:8081/static/css/clockpicker.css'
    ],
    ClockPicker: [
        //'//movies.kyngas.eu/static/js/custom/clockpicker.js'
        '//localhost:8081/static/js/custom/clockpicker.js'
    ]
}, {
    shim: {
        base_css: ['materialize_css'],
        Materialize: ['jQuery'],
        ClockPicker_css: ['materialize_css'],
        ClockPicker: ['Materialize']
    }
});

// Enables various UI functionality after css and javascript have been loaded.
fallback.ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
    $(".datepicker").pickadate({ //calendar initialization
        //http://amsul.ca/pickadate.js/date/#options
        selectMonths: true,
        selectYears: 10
    });
    $('.tooltipped').tooltip({ //tooltips initialization
        delay: 150,
        position: 'top',
        html: true
    });
    $('.collapsible').collapsible();
    $('body').addClass('loaded'); //remove loader
    $('.modal').modal(); //movies modal initialization
    $('.timepicker').pickatime({
        autoclose: true,
        twelvehour: false,
        default: 'now'
    });
});

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