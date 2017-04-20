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
       var cardId = 'img_' + info['SeriesId'];
       var cardIdTitle = 'title_' + info['SeriesId'];
       var cardIdEpisodes = 'episodes_' + info['SeriesId'];
       seenSeriesContainer.append(
           $.parseHTML(
                '<div class="col s12 m6 l3">' +
                    '<div class="card horizontal">' +
                        '<div class="card-image">' +
                            '<img id="' + cardId + '" class="series-poster">' +
                        '</div>' +
                        '<div class="card-stacked truncate">' +
                            '<div class="card-content">' +
                                '<span class="truncate content-key" id="' + cardIdTitle + '"></span>' +
                                info['SeriesId'] +
                            '</div>' +
                            '<div class="card-action">' +
                                '<span class="truncate" id="' + cardIdEpisodes + '"></span>' +
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</div>'
           )
       );
       var imgCard = $(document.getElementById(cardId));
       var titleCard = $(document.getElementById(cardIdTitle));
       var episodesCard = $(document.getElementById(cardIdEpisodes));
       decorateSeriesCard(imgCard, titleCard, episodesCard, info);
    });
    seenSeriesColl.collapsible('open', 0);
}

function decorateSeriesCard(card, titleCard, episodeCard, info) {
    if (info['Image'] !== '') {
        var path = 'https://image.tmdb.org/t/p/w300' + info['Image'];
        card.attr('src', path);
    } else {
        card.attr('src', '/static/img/nanPosterBig.jpg')
    }
    titleCard.append(info['Title']);
    episodeCard.append(
        $.parseHTML(
            '<span class="episodes-count">' +  info['Count'] + '</span>' + '<span class="episodes-seen">' + 'episodes seen' + '</span>'
        )
    );
}