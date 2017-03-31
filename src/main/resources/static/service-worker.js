var log = console.log.bind(console);
var version = "0.0.4";
var cacheName = "moviediary";
var cache = cacheName + "-" + version;
var filesToCache = [
    '/',
    'css/custom/loader.min.css',
    'css/font-awesome/css/font-awesome.min.css',
    'css/font-awesome/fonts/',
    'css/custom/materialize.min.css',
    'css/fonts/roboto/',
    'css/custom/base.css',
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
    'js/pages/login.js',
    'js/pages/movies.js',
    'js/pages/stats.js',
    'js/pages/user.js',
    'js/pages/wishlist.js',
    'js/chart.min.js',
    'js/common.js',
    'js/jquery.min.js',
    'js/load-worker.js',
    'js/loaded.js',
    'js/sockjs.min.js'
];

self.addEventListener('install', function (event) {
    log('[ServiceWorker] Installing...');
    event.waitUntil(caches
        .open(cache)
        .then(function (cache) {
            log('[ServiceWorker] Caching files');
            return cache.addAll(filesToCache);
        }));
});

self.addEventListener('fetch', function () {
    event.respondWith(caches
        .match(event.request)
        .then(function (response) {
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
    event.waitUntil(caches.keys()
        .then(function (keyList) {
            return Promise.all(keyList.map(function (key) {
                if (key !== cacheName) {
                    log('[ServiceWorker] Removing old cache ', key);
                    return caches.delete(key);
                }
            }));
        })
        .then(function () {
            self.clients.claim();
        }));
});