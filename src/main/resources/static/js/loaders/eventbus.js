fallback.load({
    SockJS: [
        '//cdnjs.cloudflare.com/ajax/libs/sockjs-client/0.3.4/sockjs.min.js',
        '//movies.kyngas.eu/static/js/sockjs-0.3.4.min.js',
        '//localhost:8081/static/js/sockjs-0.3.4.min.js'
    ],
    EventBus: [
        '//movies.kyngas.eu/static/js/vertx-eventbus.js',
        '//localhost:8081/static/js/vertx-eventbus.js'
    ]
}, {
    shim: {
        EventBus: ['SockJS']
    }
});