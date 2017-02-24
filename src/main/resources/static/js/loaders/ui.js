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
        '//movies.kyngas.eu/static/css/custom/base.css',
        '//localhost:8081/static/css/custom/base.css'
    ],
    jQuery: [
        '//cdnjs.cloudflare.com/ajax/libs/jquery/2.2.4/jquery.min.js',
        '//movies.kyngas.eu/static/js/jquery-2.2.4.min.js',
        '//localhost:8081/static/js/jquery-2.2.4.min.js'
    ],
    Materialize: [
        '//cdnjs.cloudflare.com/ajax/libs/materialize/0.98.0/js/materialize.min.js',
        '//movies.kyngas.eu/static/js/materialize.min.js',
        '//localhost:8081/static/js/materialize.min.js'
    ]
}, {
    shim: {
        //base.css requires materialize.css to be loaded before etc...
        base_css: ['materialize_css'],
        Materialize: ['jQuery']
    }
});

// Enables sidebar functionality and disables loader after UI css and javascript have been loaded.
fallback.ready(function () {
    $(".sidebar-collapse").sideNav();
    $('body').addClass('loaded');
});