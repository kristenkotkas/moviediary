fallback.ready(['jQuery', 'SockJS', 'EventBus'], function () {
    var eventbus = new EventBus("/eventbus");
    eventbus.onopen = function () {
        var lang;
        eventbus.send("translations", getCookie("lang"), function (error, reply) {
            lang = reply.body;
            console.log(lang);
            $("#Search").click(function () {
                $("#load-more").show();
                eventbus.send("database_get_history",
                    {
                        'is-first': $("#seenFirst").is(':checked'),
                        'is-cinema': $("#wasCinema").is(':checked'),
                        'start': $("#startingDay").pickadate('picker').get(),
                        'end': $("#endDay").pickadate('picker').get(),
                        'page': 0
                    }, function (error, reply) {
                        console.log(reply);
                        var data = reply.body['rows'];
                        console.log(data.length);
                        if (data.length > 0) {
                            $("#viewsTitle").empty();
                            $("#table").empty();
                            /*$("#table").empty().append(
                             '<tr>' +
                             '<th class="table-row">' + lang['HISTORY_TITLE'] + '</th>' +
                             '<th>' + lang['HISTORY_DATE'] + '</th>' +
                             '<th>' + lang['HISTORY_TIME'] + '</th>' +
                             '<th class="hide-on-med-and-down"></th>' +
                             '<th class="hide-on-small-only"></th>' +
                             '<th class="hide-on-small-only"></th>' +
                             '</tr>');*/

                            addHistory(data, lang);

                            if (data.length == 10) {
                                $("#load-more-holder").empty().append(
                                    $.parseHTML(
                                        '<tr tabindex="8" class="load-more" id="load-more">' +
                                        '<td></td>' +
                                        '<td>' + lang['HISTORY_LOAD_MORE'] + '</td>' +
                                        '</tr>'
                                    )
                                ).show();
                            } else {
                                $("#load-more-holder").empty();
                            }

                            var i = 0;

                            $("#load-more").keyup(function (e) {
                                if (e.keyCode == 13) {
                                    $("#load-more").click();
                                }
                            });

                            $("#load-more").click(function () {
                                eventbus.send("database_get_history",
                                    {
                                        'is-first': $("#seenFirst").is(':checked'),
                                        'is-cinema': $("#wasCinema").is(':checked'),
                                        'start': $("#startingDay").pickadate('picker').get(),
                                        'end': $("#endDay").pickadate('picker').get(),
                                        'page': ++i
                                    }, function (error, reply) {
                                        var addData = reply.body['rows'];
                                        console.log(addData.length);
                                        if (addData.length < 10) {
                                            $("#load-more-holder").hide();
                                        }
                                        addHistory(addData, lang);
                                        $(document).scrollTop($(document).height());
                                    });
                            });

                        } else {
                            $("#table").empty();
                            $("#viewsTitle").empty().append(
                                '<div class="card z-depth-0">' +
                                '<div class="card-title">' +
                                '<a class="light grey-text text-lighten-1 not-found">' + lang['HISTORY_NOT_PRESENT'] + '</a>' +
                                '</div>' +
                                '</div>'
                            );
                        }
                    });
            });
        });
    };
});

function addHistory(data, lang) {
    $.each(data, function (i) {
        var posterPath = "";
        if (data[i]['Image'] != "") {
            posterPath = 'https://image.tmdb.org/t/p/w342' + data[i]['Image'];
        } else {
            posterPath = '/static/img/nanPosterBig.jpg'
        }
        $("#table").append(
            $.parseHTML(
                '<li class="z-depth-0 search-object">' +
                    '<div class="collapsible-header content-key grey-text">' +
                        data[i]['Title'] +
                    '</div>' +
                    '<div class="collapsible-body white">' +
                        '<div class="row search-image">' +
                            '<div class="col m4 l3 search-image hide-on-small-only">' +
                                '<img src="' + posterPath + '" alt="Poster for movie: ' +
                                data[i]['Title'] + '" width="80%">' +
                            '</div>'+
                            '<div class="col">' +
                                '<ul>' +
                                    '<li>' +
                                    '<h4 class="grey-text">' + getMonth(data[i]['Start'], lang) + '</h4></li>' +
                                    '<li><i class="fa fa-clock-o left grey-text" aria-hidden="true"></i>' +
                                    '<span class="content-key grey-text">' + data[i]['Time'] + '</span></li>' +
                                    '<li><span class="content-key grey-text">' + lang[data[i]['DayOfWeek']] + '</span></li>' +
                                    '<li><i class=' + data[i]['WasFirst'] + ' aria-hidden="true"></i></li>' +
                                    '<li><i class=' + data[i]['WasCinema'] + ' aria-hidden="true"></i></li>' +
                                '</ul>' +
                            '</div>' +
                            '<div class="row"></div>' +
                            '<div class="row">' +
                                '<div class="col s12 m12 l12">' +
                                '<div>' +
                                'Lorem ipsum dolor sit amet, consectetur adipiscing elit, sed do eiusmod tempor incididunt ut labore et dolore magna aliqua. Ut enim ad minim veniam, quis nostrud exercitation ullamco laboris nisi ut aliquip ex ea commodo consequat.'+
                                '</div>' +
                                '</div>'+
                            '</div>' +
                        '</div>' +
                    '</div>' +
                '</li>'
            )
        );
    });
}