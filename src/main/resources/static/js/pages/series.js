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
       //console.log('series', seriesData[i]);
       var info = seriesData[i];
       var cardId = 'card_' + info['SeriesId'];
       seenSeriesContainer.append(
           $.parseHTML(
                '<div class="col s6 m4 l2">' +
                    '<div class="card" id="' + cardId + '">' +
                        '<div class="card-content truncate content-key"></div>' +
                    '</div>' +
                '</div>'
           )
       );
       var card = $(document.getElementById(cardId));
       changeToActive(card, info['Image'], info['Title']);
    });
    seenSeriesColl.collapsible('open', 0);
}

function changeToActive(card, data, title) {
    if (data !== '') {
        var path = 'https://image.tmdb.org/t/p/w300' + data;
        card
            .css("background-image", "url(" + path + ")")
            .css("background-size", "cover")
            .css("background-position", "center");
        card.addClass('white-text');
    } else {
        card.addClass('green').addClass('lighten-2').addClass('white-text');
        card.children(0).append(title);
    }
    card.css("height", "18em");
}