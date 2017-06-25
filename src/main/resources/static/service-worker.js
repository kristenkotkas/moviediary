var log = console.log.bind(console);
var version = "0.0.6";
var cacheName = "moviediary";
var cache = cacheName + "-" + version;
var filesToCache = [
    '/',
    'css/custom/base.css',
    'css/custom/loader.min.css',
    'css/custom/page-center.css',
    'css/clockpicker.css',
    'img/eng.svg',
    'img/est.svg',
    'img/nanPosterBig.jpg',
    'img/nanPosterSmall.jpg',
    'js/custom/loader.js',
    'js/custom/clockpicker.js',
    'js/custom/vertx-eventbus.js',
    'js/pages/apiUtils.js',
    'js/pages/discover.js',
    'js/pages/history.js',
    'js/pages/home.js',
    'js/pages/lists.js',
    'js/pages/login.js',
    'js/pages/movies.js',
    'js/pages/series.js',
    'js/pages/stats.js'
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