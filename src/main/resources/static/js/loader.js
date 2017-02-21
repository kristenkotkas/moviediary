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
    materialize: [
        '//localhost:8081/static/js/materialize.min.js',
        '//cdnjs.cloudflare.com/ajax/libs/materialize/0.98.0/js/materialize.min.js',
        '//movies.kyngas.eu/static/js/materialize.min.js'
    ],
    plugins: [
        '//localhost:8081/static/js/plugins.min.js',
        '//demo.geekslabs.com/materialize-v1.0/js/plugins.min.js',
        '//movies.kyngas.eu/static/js/plugins.min.js'
    ],
    SockJS: [
        '//localhost:8081/static/js/sockjs-0.3.4.min.js',
        '//cdnjs.cloudflare.com/ajax/libs/sockjs-client/0.3.4/sockjs.min.js',
        '//movies.kyngas.eu/static/js/sockjs-0.3.4.min.js'
    ],
    EventBus: [
        '//localhost:8081/static/js/vertx-eventbus.js',
        '//raw.githubusercontent.com/vert-x3/vertx-examples/master/web-examples/src/main/java/io/vertx/example/web/chat/webroot/vertx-eventbus.js',
        '//movies.kyngas.eu/static/js/vertx-eventbus.js'
    ]
}, {
    shim: {
        base_css: ['materialize_css'],
        materialize: ['jQuery'],
        plugins: ['jQuery', 'materialize'],
        EventBus: ['SockJS']
    }
});