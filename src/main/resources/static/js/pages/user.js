$(document).ready(function () {
    $(".sidebar-collapse").sideNav(); //sidebar initialization
    initMap();
});
var eventbus = new EventBus("/eventbus");
var search = $("#tv-search");
var tvTitle = $("#tv-title");
var tvTable = $("#tv-table");
var seenEpisodes = '';
eventbus.onopen = function () {
    eventbus.registerHandler("messenger", function (err, msg) {
        console.log(msg);
        Materialize.toast(msg.headers.name + ": " + msg.body, 2500);
    });
    var sendMessage = function () {
        var input = $("#MessageInput").val();
        eventbus.publish("messenger", safe_tags_replace(input));
    };
    $("#MessageInput").keyup(function (e) {
        if (e.keyCode === 13) {
            sendMessage();
        }
    });
    $("#SendMessage").click(function () {
        sendMessage();
    });
    eventbus.send("translations", getCookie("lang"), function (error, reply) {
        lang = reply.body;

        var lang;
        $.ajax({
            url: '/private/api/v1/user/info',
            type: 'GET',
            contentType: 'application/xml',
            success: function (data) {
                var $xml = $($.parseXML(data));
                addUserInfo('ID', $xml.find('id').text());
                addUserInfo(lang['USER_FIRSTNAME'], $xml.find('firstname').text());
                addUserInfo(lang['USER_LASTNAME'], $xml.find('lastname').text());
                addUserInfo(lang['USER_USERNAME'], $xml.find('username').text());
                addUserInfo(lang['USER_RUNTIME_TYPE'], $xml.find('runtimeType').text());
                addUserInfo(lang['USER_VERIFIED'], $xml.find('verified').text());
            },
            error: function (e) {
                console.log(e.message());
                Materialize.toast("Failed to query user data.", 2000);
            }
        });
    });

    search.keyup(function (e) {
        if (e.keyCode === 13) {
            fillTVTable(search.val());
        }
    });
};

var initMap = function () {
    var liivi2 = {
        lat: 58.378367,
        lng: 26.714695
    };
    var map = new google.maps.Map(document.getElementById('map'), {
        zoom: 18,
        center: liivi2
    });
    var marker = new google.maps.Marker({
        position: liivi2,
        map: map
    });
};

$.ajax({
    url: '/private/api/v1/views/count',
    type: 'GET',
    contentType: 'application/json',
    success: function (data) {
        $('#UserCount').text("User Count: " + data);
    },
    error: function (e) {
        console.log(e.message())
    }
});

function addUserInfo(key, value) {
    $("#my-info").append(
        '<tr>' +
        '<td class="grey-text">' + key + '</td>' +
        '<td class="content-key grey-text truncate">' + value + '</td>' +
        '</tr>'
    );
}

function fillTVTable(tvId) {
    console.log("id", tvId);

    eventbus.send("api_get_tv", tvId, function (error, reply) {
        var data = reply['body'];
        eventbus.send("database_get_seen_episodes", tvId, function (error, reply) {
            seenEpisodes = reply['body']['episodes'];
            console.log('data', data);
            tvTitle.empty().append(data['name']);
            tvTable.empty();
            for (var i = 0; i <= data['number_of_seasons']; i++) {
                //console.log(('season_' + i), data[('season/' + i)]);
                if (data[('season/' + i)] != null) {
                    var seasonData = data[('season/' + i)];
                    console.log(seasonData);
                    tvTable.append(
                        $.parseHTML(
                            '<li>' +
                            '<div class="collapsible-header collapsible-header-tv history-object grey-text">' +
                            '<div class="row"></div>' +
                            '<div class="row">' +
                            '<div class="col s3 m2 l1">' +
                            '<img src="' + getImageUrl(seasonData) + '" width="100%">' +
                            '</div>' +
                            '<div class="col s9 m10 l10">' +
                            '<span class="tv-season-title grey-text text-darken-3">' + seasonData['name'] + '</span>' +
                            '<span class="season-add-info grey-text text-darken-2">' +
                            getYear(seasonData['air_date'])  +
                                ' | ' +
                            seasonData['episodes'].length + ' episodes' +
                            '</span><br>' +
                            '<span class="description hide-on-med-and-down">' + seasonData['overview'] + '</span>' +
                            '</div>' +
                            '</div>' +
                            '</div>' +
                            '</div>' +
                            '<div class="collapsible-body grey lighten-4">' +
                            '<div class="row">' +
                            '<div class="col s12 m12 l12" id="episode_container_' + i + '">' +
                            '</div>' +
                            '</div>' +
                            '</div>' +
                            '</li>'
                        )
                    );
                    getEpisodes(seasonData['episodes'], i, data);
                }
            }
        });
    });
}

function getYear(airDate) {
    if (airDate != null) {
        return (airDate.split('-')[0]);
    } else return '';
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

function changeToActive(card, element, data) {
    if (data['still_path'] !== null) {
        var path = 'https://image.tmdb.org/t/p/w300' + data['still_path'];
        card
            .css("background-image", "url(" + path + ")")
            .css("background-size", "cover");
        card.addClass('white-text');
    } else {
        card.addClass('green').addClass('lighten-2').addClass('white-text');
    }
    element.append('<i class="fa fa-check fa-2x white-text" aria-hidden="true"></i>');
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

function getImageUrl(data) {
    var posterPath;
    if (data['poster_path'] != null) {
        posterPath = 'https://image.tmdb.org/t/p/w342' + data['poster_path'];
    } else {
        posterPath = '/static/img/nanPosterBig.jpg'
    }
    return posterPath;
}