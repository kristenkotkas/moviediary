fallback.load({
    SockJS: [
        '//cdnjs.cloudflare.com/ajax/libs/sockjs-client/1.1.2/sockjs.min.js',
        '//movies.kyngas.eu/static/js/sockjs.min.js',
        '//localhost:8081/static/js/sockjs.min.js'
    ],
    EventBus: [
        '//movies.kyngas.eu/static/js/custom/vertx-eventbus.js',
        '//localhost:8081/static/js/custom/vertx-eventbus.js'
    ]
}, {
    shim: {
        EventBus: ['SockJS']
    }
});