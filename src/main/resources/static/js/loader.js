var log = console.log.bind(console);
var loadingCounter = 0;
var loadingStarted = false;

function removeLoader() {
    document.getElementsByTagName('body')[0].className += " loaded";
}

function asyncLoadCSS(href) {
    loadingCounter++;
    loadingStarted = true;
    var ss = document.createElement('link');
    ss.href = href;
    ss.rel = 'stylesheet';
    ss.type = 'text/css';
    ss.media = 'bogus'; //fetch without blocking
    ss.onload = function () {
        ss.media = 'screen'; //render after loaded
        loadingCounter--;
        if (loadingCounter === 0) {
            removeLoader();
        }
    };
    document.getElementsByTagName('head')[0].appendChild(ss);
}

if ('serviceWorker' in navigator) {
    window.addEventListener('load', function () {
        navigator.serviceWorker
            .register('/static/service-worker.js', {scope: '/static/'})
            .then(function () {
                log('Service worker registered');
            })
            .catch(function (err) {
                log('Error registering service worker: ' + err);
            });
    });
} else {
    log("service workers not supported")
}

asyncLoadCSS('/static/css/font-awesome/css/font-awesome.min.css');
asyncLoadCSS('/static/css/custom/materialize.min.css');
asyncLoadCSS('/static/css/custom/base.css');
asyncLoadCSS('/static/css/custom/page-center.css');