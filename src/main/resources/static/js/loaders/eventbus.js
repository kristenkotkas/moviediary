fallback.load({
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
        EventBus: ['SockJS']
    }
});