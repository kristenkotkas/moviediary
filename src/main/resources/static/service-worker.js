var log = console.log.bind(console);
var version = "0.0.5";
var cacheName = "moviediary";
var cache = cacheName + "-" + version;
var filesToCache = [
    '/',
    'css/custom/loader.min.css',
    'css/font-awesome/css/font-awesome.min.css',
    'css/font-awesome/fonts/fontawesome-webfont.woff2?v=4.7.0',
    'css/custom/materialize.min.css',
    'css/fonts/roboto/Roboto-Regular.woff2',
    'css/fonts/roboto/Roboto-Medium.woff2',
    'css/fonts/roboto/Roboto-Light.woff2',
    'css/custom/base.css',
    'js/custom/loader.js',
    'css/custom/page-center.css',
    'css/clockpicker.css',
    'img/eng.svg',
    'img/est.svg',
    'img/ger.svg',
    'img/nanPosterBig.jpg',
    'img/nanPosterSmall.jpg',
    'js/custom/clockpicker.js',
    'js/custom/materialize.min.js',
    'js/custom/vertx-eventbus.js',
    'js/pages/history.js',
    'js/pages/home.js',
    'js/pages/login.js',
    'js/pages/movies.js',
    'js/pages/stats.js',
    'js/pages/user.js',
    'js/pages/wishlist.js',
    'js/chart.min.js',
    'js/jquery.min.js',
    'js/jquery-matchHeight.min.js',
    'js/sockjs.min.js'
];

self.addEventListener('install', function (event) {
    log('[ServiceWorker] Installing...');
    event.waitUntil(caches.open(cache).then(function (cache) {
        log('[ServiceWorker] Caching files');
        return cache.addAll(filesToCache);
    }));
});

self.addEventListener('fetch', function () {
    event.respondWith(caches.match(event.request).then(function (response) {
        if (response) {
            log("Fulfilling " + event.request.url + " from cache.");
            return response;
        }
        log(event.request.url + " not found in cache fetching from network.");
        var fetchRequest = event.request.clone();
        return fetch(fetchRequest).then(function (response) {
            if (!response || response.status !== 200 || response.type !== 'basic') {
                return response;
            }
            var responseToCache = response.clone();
            caches.open(cacheName).then(function (cache) {
                cache.put(event.request, responseToCache);
            });
            return response;
        });
    }));
});

self.addEventListener('activate', function (event) {
    log('[ServiceWorker] Activate');
    event.waitUntil(caches.keys().then(function (keyList) {
        return Promise.all(keyList.map(function (key) {
            if (key !== cacheName) {
                log('[ServiceWorker] Removing old cache ', key);
                return caches.delete(key);
            }
        }));
    }).then(function () {
        self.clients.claim();
    }));
});