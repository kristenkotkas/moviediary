$(document).ready(function () {
    $('.tooltipped').tooltip({ //tooltips initialization
        delay: 150,
        position: 'top',
        html: true
    });
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});
var eventbus = new EventBus("/eventbus");
var search = $("#series-search");
var seenSeriesColl = $("#seen-series-coll");
var seenSeriesContainer = $("#seen-series-container");

eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;

        eventbus.send("database_get_watching_series", {},  function (error, reply) {
            var data = reply.body['rows'];
            console.log('seenSeries', data);
            fillSeenSeries(data);
        });
    });
};

function fillSeenSeries(seriesData) {
    seenSeriesContainer.empty();
    $.each(seriesData, function (i) {
       console.log('series', seriesData[i]);
       var info = seriesData[i];
       seenSeriesContainer.append(
           $.parseHTML(
                '<div class="col s6 m4 l2">' +
                    '<div class="card">' +
                        '<div class="card-content">' +
                            '<img src="' + getImageUrl(info['Image']) + '">' +
                        '</div>' +
                    '</div>' +
                '</div>'
           )
       );
    });
    seenSeriesColl.collapsible('open', 0);
}

function getImageUrl(data) {
    var posterPath;
    if (data != "") {
        posterPath = 'https://image.tmdb.org/t/p/w342' + data;
    } else {
        posterPath = '/static/img/nanPosterBig.jpg'
    }
    return posterPath;
}