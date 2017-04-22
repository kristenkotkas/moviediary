$(document).ready(function () {
    $('.tooltipped').tooltip({ //tooltips initialization
        delay: 150,
        position: 'top',
        html: true
    });
    $(".sidebar-collapse").sideNav(); //sidebar initialization
});
var eventbus = new EventBus("/eventbus");
var searchBar = $("#series-search");
var searchButton = $("#series-search-button");
var searchResultContainer = $("#tv-search-result-container");
var seenSeriesColl = $("#seen-series-coll");
var seenSeriesContainer = $("#seen-series-container");
var loaderContainer = $("#tv-loader");
var seriesTitle = $("#series-title");
var navbar = $("#navbar-background");
var body = $("#body");
var seriesDataContainer = $("#series-data-container");
var seenEpisodes;
var seenSeriesHeader = $("#seen-series-header");

eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;
        enableParameterSeriesLoading(eventbus, lang);

        eventbus.send("database_get_watching_series", {},  function (error, reply) {
            var seenSeries = reply.body['rows'];
            //console.log('seenSeries', data);
            fillSeenSeries(seenSeries);
            openSeenSeries();

            searchBar.keyup(function (e) {
                if (e.keyCode === 13) {
                    searchButton.click();
                }
            });
            searchButton.click(function () {
                getSeriesSearch();
            });

            seenSeriesHeader.click(function () {
                eventbus.send("database_get_watching_series", {},  function (error, reply) {
                    fillSeenSeries(reply.body['rows']);
                });
            })
        });
    });
};

function startLoading() {
    loaderContainer.append('<i class="fa fa-circle-o-notch fa-spin fa-3x fa-fw"></i>');
}

function endLoading() {
    loaderContainer.empty();
}

var enableParameterSeriesLoading = function (eventbus, lang) {
    var loadSeries = function (eventbus, lang) {
        var query = getUrlParam("id");
        if (query !== null && isNormalInteger(query)) {
            openSeries(parseInt(query), 1);
        }
    };
    window.onpopstate = function () { //try to load series on back/forward page movement
        loadSeries(eventbus, lang);
    };
    loadSeries(eventbus, lang); //load series if url has param
};

function getSeriesSearch() {
    eventbus.send("api_get_tv_search", searchBar.val(), function (error, reply) {
        seriesDataContainer.empty();
        closeSeenSeries();
        var searchResult = reply.body['results'];
        searchResultContainer.empty();
        $.each(searchResult, function (i) {
            var searchedTvSeries = searchResult[i];
            var resultCardId = 'result_search' + searchedTvSeries['id'];
            console.log(searchedTvSeries);
            searchResultContainer.append(
                $.parseHTML(
                    '<div class="col s12 m6 l4">' +
                        '<div class="card horizontal z-depth-0 search-object-series" id="' + resultCardId + '">' +
                            '<div class="card-image">' +
                                '<img src="' + getPosterPath(searchedTvSeries['poster_path']) + '" class="series-poster">' +
                            '</div>' +
                            '<div class="card-stacked truncate">' +
                                '<div class="card-content truncate">' +
                                    '<span class="truncate series-search-title">' + searchedTvSeries['name'] + '</span>' +
                                    '<span class="truncate">' +
                                        '<i class="fa fa-calendar-o" aria-hidden="true"></i>' +
                                        '<span class="series-date">' + getFirstAirDate(searchedTvSeries['first_air_date']) + '</span>' +
                                    '</span>' +
                                '</div>' +
                            '</div>' +
                        '</div>' +
                    '</div>'
                )
            );
            var resultCard = $(document.getElementById(resultCardId));

            resultCard.click(function () {
                console.log(resultCardId + ' clicked');
                openSeries(searchedTvSeries['id'], 1);
                startLoading();
            })
        })
    });
}

function openSeries(seriesId, page) {
    console.log('opened series', seriesId);
    eventbus.send('api_get_tv',
        {
            seriesId: seriesId,
            page: page
        }
    , function (error, reply) {
        closeSeenSeries();
        searchResultContainer.empty();

        var seriesData = reply['body'];

        console.log('seriesData', seriesData);
        changeDesign(seriesData);
        fillResultSeries(seriesData, page);
            replaceUrlParameter("id", seriesId);
    });
    //page = 1;
}

function fillResultSeries(seriesData, page) {
    endLoading();
    eventbus.send("database_get_seen_episodes", seriesData['id'], function (error, reply) {
        seriesDataContainer.empty();

        if (seriesData['number_of_seasons'] > 9) {
            var neededPages = Math.floor(seriesData['number_of_seasons'] / 10) + 1;

            $.each(new Array(neededPages), function (pages) {
                var id = 'series-page_' + (pages + 1);
                seriesDataContainer.append(
                    $.parseHTML(
                        '<a class="btn waves-effect light-blue" id="' + id + '">' + (pages + 1) + '</a>'
                    )
                );

                $(document.getElementById(id)).click(function () {
                    openSeries(seriesData['id'], pages + 1);
                    console.log('page', pages + 1);
                })
            });

        }

        seenEpisodes = reply['body']['episodes'];
        for (var i = ((page - 1) * 10); i < page * 10; i++) {
            var season = 'season/' + i;
            if (seriesData[season] != null) {
                var seasonData = seriesData[season];
                console.log(seasonData);
                seriesDataContainer.append(
                    $.parseHTML(
                        '<li>' +
                            '<div class="collapsible-header collapsible-header-tv history-object grey-text">' +
                                '<div class="row last-row">' +
                                    '<div class="col s3 m2 l1">' +
                                        '<img src="' + getPosterPath(seasonData['poster_path']) + '" width="100%">' +
                                    '</div>' +
                                    '<div class="col s9 m10 l11">' +
                                        '<span class="tv-season-title grey-text text-darken-3">' + seasonData['name'] + '</span>' +
                                        '<span class="season-add-info grey-text text-darken-2">' +
                                            getYear(seasonData['air_date'])  +
                                            seasonData['episodes'].length + ' episodes' +
                                        '</span><br>' +
                                        '<span class="description hide-on-med-and-down grey-text text-darken-4" >' + seasonData['overview'] + '</span>' +
                                    '</div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="collapsible-body collapsible-body-tv grey lighten-4">' +
                                '<div class="row">' +
                                    '<div class="col s12 m12 l12" id="episode_container_' + i + '"></div>' +
                                '</div>' +
                            '</div>' +
                        '</li>'
                    )
                );
                getEpisodes(seasonData['episodes'], i, seriesData);
            }
        }
    });
}


function getEpisodes(episodes, seasonNumber, seriesData) {
    var elem = $(document.getElementById('episode_container_' + seasonNumber));
    elem.empty();
    for (var episode = 1; episode <= episodes.length; episode++) {
        var id = 's' + seasonNumber + 'e' + episode;
        var episodeData = episodes[episode - 1];
        var seasonData = seriesData[('season/' + seasonNumber)];
        elem.append(
            '<div class="col s12 m6 l3 grey-text">' +
                '<div class="card cursor episode-card" id="' + id + '">' +
                    '<div class="card-content">' +
                        '<div class="content-key truncate">' + episodeData['name'] + '</div>' +
                            '<span class="badge" id="' + (id + '_check') + '"></span>' +
                        '<div class="">' + 'Ep: ' + episodeData['episode_number'] + '</div>' +
                        '<div class="smaller">' + episodeData['air_date'] + '</div>' +
                    '</div>' +
                '</div>' +
            '</div>'
        );

        if (jQuery.inArray(episodeData['id'], seenEpisodes) !== -1) {
            changeToActive(
                $(document.getElementById(id)),
                $(document.getElementById(id + '_check')),
                episodeData);
            console.log('inarray');
        }
        $(document.getElementById(id)).click(
            {
                param: episodeData,
                id: id,
                seriesData: seriesData,
                seasonData: seasonData
            }
            , cardOnClick);
    }
}

function cardOnClick(event) {
    addEpisode(
        $(document.getElementById(event.data.id)),
        $(document.getElementById(event.data.id + '_check')),
        event.data.param,
        event.data.seriesData,
        event.data.seasonData
    );
}

function addEpisode(card, element, data, seriesData, seasonData) {
    if (element.children().length === 0) {
        addEpisodeToView(data, seriesData, seasonData, card, element);
    } else {
        removeEpisode(card, element, data);
    }
}

function changeToInActive(card, element, data) {
    card.removeAttr('style');
    card.removeClass('green').removeClass('white-text').addClass('grey-text');
    element.empty();
}

function addEpisodeToView(episodeData, seriesData, seasonData, card, element) {
    var episodeId = episodeData['id'];
    var seriesId = seriesData['id'];
    console.log('seriesId', seriesId);
    console.log('episodeId', episodeId);
    eventbus.send("database_insert_episode",
        {
            'seriesId': seriesId,
            'episodeId': episodeId,
            'seasonId': seasonData['_id']
        }
        , function (error, reply) {
            console.log('reply', reply);
            if (reply['body']['updated'] != null) {
                console.log('episode added');
                changeToActive(card, element, episodeData);
            }
        });
}

function removeEpisode(card, element, episodeData) {
    var episodeId = episodeData['id'];
    eventbus.send("database_remove_episode",
        episodeId
        , function (error, reply) {
            console.log('reply', reply);
            if (reply['body']['updated'] != null) {
                console.log('episode removed');
                changeToInActive(card, element, episodeData);
            }
        });
}

function changeToActive(card, element, data) {
    card.addClass('green').addClass('lighten-2').addClass('white-text');
    if (data['still_path'] !== null) {
        var path = 'https://image.tmdb.org/t/p/w300' + data['still_path'];
        card
            .css("background-image", "url(" + path + ")")
            .css("background-size", "cover");
        card.addClass('white-text');
    }
    element.append('<i class="fa fa-check fa-2x white-text" aria-hidden="true"></i>');
}

function changeDesign(seriesData) {
    seriesTitle.text(seriesData['name']).addClass('movies-heading');
    navbar.removeClass('cyan').addClass('transparent');
    body.attr("background", getBackdropPath(seriesData['backdrop_path']));
}

function fillSeenSeries(seriesData) {
    seenSeriesContainer.empty();
    $.each(seriesData, function (i) {
       //console.log('series', seriesData[i]);
       var info = seriesData[i];
       var cardId = 'img_' + info['SeriesId'];
       var cardIdTitle = 'title_' + info['SeriesId'];
       var cardIdEpisodes = 'episodes_' + info['SeriesId'];
       var resultCardId = 'result_' + info['SeriesId'];
       seenSeriesContainer.append(
           $.parseHTML(
                '<div class="col s12 m6 l4">' +
                    '<div class="card horizontal z-depth-0 search-object-series" id="' + resultCardId + '">' +
                        '<div class="card-image">' +
                            '<img id="' + cardId + '" class="series-poster">' +
                        '</div>' +
                        '<div class="card-stacked truncate">' +
                            '<div class="card-content">' +
                                '<span class="truncate content-key" id="' + cardIdTitle + '"></span>' +
                                //info['SeriesId'] +
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
       var resultCard = $(document.getElementById(resultCardId));

        resultCard.click(function () {
            openSeries(info['SeriesId'], 1);
            startLoading();
        });

       decorateSeriesCard(imgCard, titleCard, episodesCard, info);
    });
}

function getYear(airDate) {
    if (airDate != null) {
        return (airDate.split('-')[0] + ' | ');
    } else return '';
}

function getPosterPath(posterPath) {
    if (posterPath != null) {
        return 'https://image.tmdb.org/t/p/w300' + posterPath;
    } else {
        return '/static/img/nanPosterBig.jpg'
    }
}

function getBackdropPath(backdropPath) {
    if (backdropPath === null) {
        return "";
    } else {
        return 'https://image.tmdb.org/t/p/w1920' + backdropPath;
    }
}

function getFirstAirDate(firstAirDate) {
    if (firstAirDate != '') {
        return firstAirDate;
    } else {
        return ' ';
    }
}

function openSeenSeries() {
    seenSeriesColl.collapsible('open', 0);
}

function closeSeenSeries() {
    seenSeriesColl.collapsible('close', 0);
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