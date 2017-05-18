$("#navbar-series").addClass('navbar-text-active');
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
var isMobile = false;

eventbus.onopen = function () {
    var lang;
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        if ($( window ).width() <= 600) {
            isMobile = true;
        }
        lang = reply.body;
        enableParameterSeriesLoading(eventbus, lang);

        eventbus.send("database_get_watching_series", {},  function (error, reply) {
            var seenSeries = reply.body['rows'];
            //console.log('seenSeries', data);
            fillSeenSeries(seenSeries, lang);
            openSeenSeries();

            searchBar.keyup(function (e) {
                if (e.keyCode === 13) {
                    searchButton.click();
                }
            });
            searchButton.click(function () {
                getSeriesSearch(lang);
            });

            seenSeriesHeader.click(function () {
                eventbus.send("database_get_watching_series", {},  function (error, reply) {
                    fillSeenSeries(reply.body['rows'], lang);
                });
            })
        });
    });
};

function startLoading() {
    if (loaderContainer.empty()) {
        loaderContainer.append('<i class="fa fa-circle-o-notch white-text fa-spin fa-3x fa-fw"></i>');
    }
}

function endLoading() {
    loaderContainer.empty();
}

var enableParameterSeriesLoading = function (eventbus, lang) {
    var loadSeries = function (eventbus, lang) {
        var query = getUrlParam("id");
        if (query !== null && isNormalInteger(query)) {
            openSeries(parseInt(query), 1, lang);
        }
    };
    window.onpopstate = function () { //try to load series on back/forward page movement
        loadSeries(eventbus, lang);
    };
    loadSeries(eventbus, lang); //load series if url has param
};

function getSeriesSearch(lang) {
    eventbus.send("api_get_tv_search", searchBar.val(), function (error, reply) {
        seriesDataContainer.empty();
        if (isMobile) {
            closeSeenSeries();
        }
        var searchResult = reply.body['results'];
        searchResultContainer.empty();
        $.each(searchResult, function (i) {
            var searchedTvSeries = searchResult[i];
            var resultCardId = 'result_search' + searchedTvSeries['id'];
            //console.log(searchedTvSeries);
            searchResultContainer.append(
                $.parseHTML(
                    '<div class="col s12 m12 l4">' +
                        '<div class="card horizontal z-depth-0 search-object-series" id="' + resultCardId + '">' +
                            '<div class="card-image">' +
                                '<img src="' + getPosterPath(searchedTvSeries['poster_path']) + '" class="series-poster" ' +
                                'alt="Poster for series: ' + searchedTvSeries['name'] + '">' +
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
                openSeries(searchedTvSeries['id'], 1, lang);
                startLoading();
            })
        })
    });
}

function openSeries(seriesId, page, lang) {
    //seriesDataContainer.empty();
    //closeSeenSeries();
    eventbus.send('api_get_tv',
        {
            seriesId: seriesId,
            page: page
        }
    , function (error, reply) {
        if (isMobile) {
            closeSeenSeries();
        }
        searchResultContainer.empty();

        var seriesData = reply['body'];

        //console.log('seriesData', seriesData);
        changeDesign(seriesData);
        fillResultSeries(seriesData, page, lang);
            replaceUrlParameter("id", seriesId);
            endLoading();
    });
    //page = 1;
}

function fillResultSeries(seriesData, page, lang) {
    endLoading();
    eventbus.send("database_get_seen_episodes", seriesData['id'], function (error, reply) {
        seriesDataContainer.empty();
        addPagins(seriesData, page, lang, 'top');
        seenEpisodes = reply['body']['episodes'];
        for (var i = ((page - 1) * 10); i < page * 10; i++) {
            var season = 'season/' + i;
            if (seriesData[season] != null) {
                var seasonData = seriesData[season];
                var episode = ' ';
                if (seasonData['episodes'].length > 1) {
                    episode += lang['SERIES_EPISODE_SINGULAR'];
                } else {
                    episode += lang['SERIES_EPISODE_PLURAL'];
                }
                seasonData['series_id'] = seriesData['id'];
                //console.log(seasonData);
                //console.log(seasonData);
                seriesDataContainer.append(
                    $.parseHTML(
                        '<li>' +
                            '<div class="opacity-object collapsible-header collapsible-header-tv history-object grey-text">' +
                                '<div class="row last-row">' +
                                    '<div class="col s3 m2 l1">' +
                                        '<img src="' + getPosterPath(seasonData['poster_path']) + '"' +
                                        ' alt="Poster for: ' + seasonData['name'] + '" class="img-100">' +
                                    '</div>' +
                                    '<div class="col s9 m10 l11">' +
                                        '<span class="tv-season-title grey-text text-darken-3">' + seasonData['name'] + '</span>' +
                                        '<span class="season-add-info grey-text text-darken-2">' +
                                            getYear(seasonData['air_date'])  +
                                            seasonData['episodes'].length + episode +
                                        '</span><br>' +
                                        '<span class="description hide-on-med-and-down grey-text text-darken-4" >' + seasonData['overview'] + '</span>' +
                                    '</div>' +
                                '</div>' +
                            '</div>' +
                            '<div class="collapsible-body collapsible-body-tv grey lighten-4">' +
                                '<div class="row">' +
                                    '<div class="row">' +
                                        '<a class="btn green lighten-2 z-depth-0" id="add-season-' + seasonData['_id'] + '">Add whole season</a>' +
                                    '</div>' +
                                    '<div class="row">' +
                                        '<a class="btn red lighten-2 z-depth-0" id="remove-season-' + seasonData['_id'] + '">Remove whole season</a>' +
                                    '</div>' +
                                    '<div class="col s12 m12 l12" id="episode_container_' + i + '"></div>' +
                                '</div>' +
                            '</div>' +
                        '</li>'
                    )
                );

                $(document.getElementById('add-season-' + seasonData['_id']))
                    .click({
                        seasonData: seasonData,
                        lang: lang,
                        container: $(document.getElementById('episode_container_' + i))
                    }, addSeasonToWatch);

                $(document.getElementById('remove-season-' + seasonData['_id']))
                    .click({
                        seasonData: seasonData,
                        lang: lang,
                        container: $(document.getElementById('episode_container_' + i))
                    }, removeSeasonFromWatch);

                getEpisodes(seasonData['episodes'], i, seriesData, lang);

            }
        }
        addPagins(seriesData, page, lang, 'bottom');
    });
}

function addPagins(seriesData, page, lang, type) {
    if (seriesData['number_of_seasons'] > 9) {
        var neededPages = Math.floor(seriesData['number_of_seasons'] / 10) + 1;
        seriesDataContainer.append(
            $.parseHTML(
                '<li class="z-depth-0"><ul class="pagination center pagin-container" id="' + type + '-pagins-container"></ul></li>'
            )
        );
        $.each(new Array(neededPages), function (pages) {
            var id = 'series-page_' + type + '_' + (pages + 1);
            $(document.getElementById(type + '-pagins-container')).append(
                $.parseHTML(
                    '<li class="waves-effect pagin-button" id="' + id + '"><a><span class="pagin-text">' + (pages + 1) + '</span></a></li>'
                )
            );

            $(document.getElementById(id)).click(function () {
                openSeries(seriesData['id'], pages + 1, lang);
                startLoading();
            });
            $(document.getElementById('series-page_' + type + '_' + page)).addClass('active');
        });

    }
}

function getEpisodes(episodes, seasonNumber, seriesData, lang) {
    var elem = $(document.getElementById('episode_container_' + seasonNumber));
    elem.empty();
    for (var episode = 1; episode <= episodes.length; episode++) {
        var id = 's' + seasonNumber + 'e' + episode;
        var episodeData = episodes[episode - 1];
        var seasonData = seriesData[('season/' + seasonNumber)];
        var ep = lang['SERIES_EPISODE_SHORT'] + ': ';
        var episodeCard = $.parseHTML(
            '<div class="col s12 m12 l6 xl3 grey-text">' +
                '<div class="card cursor episode-card" id="' + id + '">' +
                    '<div class="card-content">' +
                        '<div class="content-key truncate">' + episodeData['name'] + '</div>' +
                            '<span class="badge" id="' + (id + '_check') + '"></span>' +
                        '<div class="">' + ep + episodeData['episode_number'] + '</div>' +
                        '<div class="smaller">' + getNormalDate(episodeData['air_date'], lang) + '</div>' +
                    '</div>' +
                '</div>' +
            '</div>'
        );
        elem.append(episodeCard);
        $(episodeCard).data('episodeData', episodeData);

        if (jQuery.inArray(episodeData['id'], seenEpisodes) !== -1) {
            changeToActive(
                $(document.getElementById(id)),
                $(document.getElementById(id + '_check')),
                episodeData);
            //console.log('inarray');
        }
        $(document.getElementById(id)).click(
            {
                param: episodeData,
                id: id,
                seriesData: seriesData,
                seasonData: seasonData,
                lang: lang
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
        event.data.seasonData,
        event.data.lang
    );
}

function addEpisode(card, element, data, seriesData, seasonData, lang) {
    //console.log(seriesData);
    if (element.children().length === 0) {
        addEpisodeToView(data, seriesData, seasonData, card, element, lang);
    } else {
        removeEpisode(card, element, data, lang);
    }
}

function changeToInActive(card, element, data) {
    card.removeAttr('style');
    card.removeClass('green').removeClass('white-text').addClass('grey-text');
    element.empty();
}

function addEpisodeToView(episodeData, seriesData, seasonData, card, element, lang) {
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
            //console.log('reply', reply);
            if (reply['body']['updated'] != null) {
                console.log('episode added');
                changeToActive(card, element, episodeData);
                eventbus.send("database_get_watching_series", {},  function (error, reply) {
                    fillSeenSeries(reply.body['rows'], lang);
                });
            }
        });
}

function removeEpisode(card, element, episodeData, lang) {
    var episodeId = episodeData['id'];
    eventbus.send("database_remove_episode",
        episodeId
        , function (error, reply) {
            //console.log('reply', reply);
            if (reply['body']['updated'] != null) {
                console.log('episode removed');
                changeToInActive(card, element, episodeData);
                eventbus.send("database_get_watching_series", {},  function (error, reply) {
                    fillSeenSeries(reply.body['rows'], lang);
                });
            }
        });
}

function addSeasonToWatch(event) {
    var data = event.data.seasonData;
    //console.log('seasonData', data);
    eventbus.send("database_insert_season_views",
        {
            seriesId: data['series_id'].toString(),
            seasonNr: data['season_number'].toString()
        }
        , function (error, reply) {
            if (reply['body']['updated'] != null) {
                eventbus.send("database_get_watching_series", {},  function (error, reply) {
                    fillSeenSeries(reply.body['rows'], event.data.lang);
                    var episodesContainer = event.data.container;
                    var timeout = 0;
                    $.each(data['episodes'], function (i) {
                        setTimeout(function () {
                            var card = episodesContainer[0]['childNodes'][i]['childNodes'];
                            var episodeData = data['episodes'][i];
                            var element = $(card).find('span.badge');
                            /*console.log(card);
                             console.log(episodeData);
                             console.log('elemenr', element);*/
                            changeToActive($(card), element, episodeData);
                        }, timeout += 25);
                    })
                });
            }
        });
}

function removeSeasonFromWatch(event) {
    var data = event.data.seasonData;
    var seasonId = data['_id'];
    console.log(seasonId);
    eventbus.send("database_remove_season_views", seasonId, function (error, reply) {
        console.log(reply.body);
        if (reply['body']['updated'] != null) {
            eventbus.send("database_get_watching_series", {},  function (error, reply) {
                fillSeenSeries(reply.body['rows'], event.data.lang);
                var episodesContainer = event.data.container;
                var timeout = 0;
                $.each(data['episodes'], function (i) {
                    setTimeout(function () {
                        var card = episodesContainer[0]['childNodes'][i]['childNodes'];
                        var element = $(card).find('span.badge');
                        /*console.log(card);
                         console.log(episodeData);
                         console.log('elemenr', element);*/
                        changeToInActive($(card), element);
                    }, timeout += 25);
                })
            });
        }
    });
}

function changeToActive(card, element, episodeData) {
    card.addClass('green').addClass('lighten-2').addClass('white-text');
    if (episodeData['still_path'] !== null) {
        var path = 'https://image.tmdb.org/t/p/w300' + episodeData['still_path'];
        card
            .css("background-image", "url(" + path + ")")
            .css("background-size", "cover");
        card.addClass('white-text');
    }
    if (element.empty()) {
        element.append('<i class="fa fa-check fa-2x white-text" aria-hidden="true"></i>');
    }
}

function changeDesign(seriesData) {
    seriesTitle.text(seriesData['name']).addClass('movies-heading');
    //navbar.removeClass('cyan').addClass('transparent');
    body.attr("background", getBackdropPath(seriesData['backdrop_path']));
}

function fillSeenSeries(seriesData, lang) {
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
                '<div class="col s12 m12 l12">' +
                    '<div class="card horizontal z-depth-0 search-object-series" id="' + resultCardId + '">' +
                        '<div class="card-image">' +
                            '<img id="' + cardId + '" class="series-poster" alt="Poster for series: ' + seriesData[i]['Title'] + '">' +
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
            openSeries(info['SeriesId'], 1, lang);
            startLoading();
        });

       decorateSeriesCard(imgCard, titleCard, episodesCard, info, lang);
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

function decorateSeriesCard(card, titleCard, episodeCard, info, lang) {
    if (info['Image'] !== '') {
        var path = 'https://image.tmdb.org/t/p/w300' + info['Image'];
        card.attr('src', path);
    } else {
        card.attr('src', '/static/img/nanPosterBig.jpg')
    }
    titleCard.append(info['Title']);
    var episodeSeen = '';
    if (info['Count'] > 1) {
        episodeSeen = lang['SERIES_EPISODE_SEEN_PLURAL'];
    } else {
        episodeSeen = lang['SERIES_EPISODE_SEEN_SINGULAR'];
    }
    episodeCard.append(
        $.parseHTML(
            '<span class="episodes-count">' +  info['Count'] + '</span>' + '<span class="episodes-seen">' + episodeSeen + '</span>'
        )
    );
}

function getNormalDate (date, lang) {
    if (date === null) {
        return lang['MOVIES_JS_UNKNOWN'];
    } else {
        var startArray = date.split('-');
        var dateFormat = new Date(date),
            locale = "en-us";
        var month = dateFormat.toLocaleString(locale, {month: "long"});
        var weekday = new Date(startArray[0], startArray[1] - 1, startArray[2]).getDay();
        //console.log(weekday); //0=Sun, 1=Mon, ..., 6=Sat
        return getShortDayOfWeek(lang, weekday) + ' ' + startArray[2] + lang[month.toUpperCase()] + ' ' + startArray[0];
    }
}

function getShortDayOfWeek(lang, dayIndex) {
    var weekdays = [
        lang['STATISTICS_SUN'],
        lang['STATISTICS_MON'],
        lang['STATISTICS_TUE'],
        lang['STATISTICS_WED'],
        lang['STATISTICS_THU'],
        lang['STATISTICS_FRI'],
        lang['STATISTICS_SAT']
    ];

    return weekdays[dayIndex];
}