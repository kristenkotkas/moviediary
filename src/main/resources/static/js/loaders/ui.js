fallback.load({
    'font-awesome_css': [
        '//localhost:8081/static/css/font-awesome-4.7.0/css/font-awesome.min.css',
        '//cdnjs.cloudflare.com/ajax/libs/font-awesome/4.7.0/css/font-awesome.min.css',
        '//movies.kyngas.eu/static/css/font-awesome-4.7.0/css/font-awesome.min.css'
    ],
    materialize_css: [
        '//localhost:8081/static/css/materialize.min.css',
        '//cdnjs.cloudflare.com/ajax/libs/materialize/0.98.0/css/materialize.min.css',
        '//movies.kyngas.eu/static/css/materialize.min.css'
    ],
    base_css: [
        '//localhost:8081/static/css/custom/base.css',
        '//movies.kyngas.eu/static/css/custom/base.css'
    ],
    jQuery: [
        '//localhost:8081/static/js/jquery-2.2.4.min.js',
        '//ajax.googleapis.com/ajax/libs/jquery/2.2.4/jquery.min.js',
        '//cdnjs.cloudflare.com/ajax/libs/jquery/2.2.4/jquery.min.js',
        '//movies.kyngas.eu/static/js/jquery-2.2.4.min.js'
    ],
    Materialize: [
        '//localhost:8081/static/js/materialize.min.js',
        '//cdnjs.cloudflare.com/ajax/libs/materialize/0.98.0/js/materialize.min.js',
        '//movies.kyngas.eu/static/js/materialize.min.js'
    ]
    /*plugins: [
     '//localhost:8081/static/js/plugins.min.js',
     '//demo.geekslabs.com/materialize-v1.0/js/plugins.min.js',
     '//movies.kyngas.eu/static/js/plugins.min.js'
     ],*/
}, {
    shim: {
        //base.css requires materialize.css to be loaded before etc...
        base_css: ['materialize_css'],
        Materialize: ['jQuery']
        /*plugins: ['jQuery', 'Materialize'],*/
    }
});

//Enables sidebar functionality after MaterializeCSS has been loaded
fallback.ready(['Materialize'], function () {
    $(".sidebar-collapse").sideNav();
});