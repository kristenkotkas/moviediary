fallback.load({
    'font-awesome_css': [
        '//cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css',
        '//movies.kyngas.eu/static/css/font-awesome-4.7.0/css/font-awesome.min.css',
        '//localhost:8081/static/css/font-awesome-4.7.0/css/font-awesome.min.css'
    ],
    materialize_css: [
        '//cdnjs.cloudflare.com/ajax/libs/materialize/0.98.0/css/materialize.min.css',
        '//movies.kyngas.eu/static/css/materialize.min.css',
        '//localhost:8081/static/css/materialize.min.css'
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
        '//cdnjs.cloudflare.com/ajax/libs/materialize/0.98.0/js/materialize.min.js',
        '//movies.kyngas.eu/static/js/materialize.min.js',
        '//localhost:8081/static/js/materialize.min.js'
    ]
}, {
    shim: {
        base_css: ['materialize_css'],
        Materialize: ['jQuery']
    }
});

// Enables various UI functionality after css and javascript have been loaded.
fallback.ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
    $(".datepicker").pickadate({ //calendar initialization
        //http://amsul.ca/pickadate.js/date/#options
        selectMonths: true,
        selectYears: 15
    });
    $('.tooltipped').tooltip({ //tooltips initialization
        delay: 150,
        position: 'top',
        html: true
    });
    $('body').addClass('loaded'); //remove loader
});

function getCookie(name) {
    var value = "; " + document.cookie;
    var parts = value.split("; " + name + "=");
    if (parts.length == 2) return parts.pop().split(";").shift();
}

function getMonth(start, lang) {
    var startArray = start.split(' ');
    var month = startArray[1];
    return startArray[0] +  lang[month.toUpperCase()] + ' ' + startArray[2];
}