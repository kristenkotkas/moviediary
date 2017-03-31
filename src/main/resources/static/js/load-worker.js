if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
        navigator.serviceWorker
            .register('/static/service-worker.js', {scope: '/static/'})
            .then(function () {
                console.log('Service worker registered');
            })
            .catch(function (err) {
                console.log('Error registering service worker: ' + err);
            });
    });
} else {
    console.log("service workers not supported")
}